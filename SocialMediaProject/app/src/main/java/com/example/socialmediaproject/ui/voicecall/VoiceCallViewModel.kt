package com.example.socialmediaproject.ui.voicecall

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.webrtc.AudioSource
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

class VoiceCallViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    val callStatus = MutableLiveData<String>()
    var isCaller: Boolean = false

    lateinit var peerConnectionFactory: PeerConnectionFactory
    lateinit var peerConnection: PeerConnection
    lateinit var localAudioTrack: org.webrtc.AudioTrack
    lateinit var localAudioSource: AudioSource
    var roomId: String = ""

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer()
    )

    fun initPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                sendIceCandidate(candidate)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                callStatus.postValue(newState.toString())
            }

            override fun onAddStream(stream: MediaStream) {
                //do nothing
            }

            override fun onRemoveStream(p0: MediaStream?) {
                //do nothing
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                val remoteAudioTrack = transceiver.receiver.track() as? org.webrtc.AudioTrack
                remoteAudioTrack?.setEnabled(true)
                remoteAudioTrack?.setVolume(10.0)
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {

                Log.d("CALL_FLOW", "ICE connection state changed: $p0")

            }
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {}
        })!!

        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        localAudioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", localAudioSource)

        val audioTrack = localAudioTrack
        val audioSender = peerConnection.addTransceiver(
            audioTrack,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY)
        )

        Log.d("CALL_FLOW", "PeerConnection initialized")
    }

    fun startCall() {
        val mediaConstraints = MediaConstraints()
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {

                        Log.e("CALL_ERROR", "setLocalDescription failed: $p0")

                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {

                        Log.e("CALL_ERROR", "createOffer/Answer failed: $p0")

                    }
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)

                val offer = mapOf(
                    "sdp" to sdp.description,
                    "type" to sdp.type.canonicalForm()
                )
                database.child("calls/$roomId/offer").setValue(offer)

                Log.d("CALL_FLOW", "Caller: Offer created -> SDP: ${sdp.description}")

            }

            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
            override fun onCreateFailure(p0: String?) {}
        }, mediaConstraints)

        Log.d("CALL_FLOW", "Caller startCall - Local audio enabled: ${localAudioTrack.enabled()}")
    }

    fun listenForOffer() {
        database.child("calls/$roomId/offer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val offerSdp = snapshot.child("sdp").getValue(String::class.java)
                    val type = snapshot.child("type").getValue(String::class.java)

                    if (offerSdp != null && type == "offer") {
                        val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
                        peerConnection.setRemoteDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                createAnswer()
                            }

                            override fun onSetFailure(p0: String?) {}
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, offer)
                    }

                    Log.d("CALL_FLOW", "Callee: Offer received from Firebase")

                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun createAnswer() {
        val constraints = MediaConstraints()
        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)

                val answer = mapOf(
                    "sdp" to sdp.description,
                    "type" to sdp.type.canonicalForm()
                )
                database.child("calls/$roomId/answer").setValue(answer)
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
            override fun onCreateFailure(p0: String?) {}
        }, constraints)
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val candidateData = mapOf(
            "candidate" to candidate.sdp,
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex
        )
        val candidatePath = if (isCaller) "callerCandidates" else "calleeCandidates"

        Log.d("CALL_FLOW", "Sending ICE (${if (isCaller) "caller" else "callee"}): $candidateData")

        database.child("calls/$roomId/$candidatePath").push().setValue(candidateData)
    }

    fun listenForCallerCandidates() {
        database.child("calls/$roomId/callerCandidates")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val sdp = snapshot.child("candidate").getValue(String::class.java)
                    val sdpMid = snapshot.child("sdpMid").getValue(String::class.java)
                    val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java)

                    if (sdp != null && sdpMid != null && sdpMLineIndex != null) {
                        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                        peerConnection.addIceCandidate(candidate)

                        Log.d("CALL_FLOW", "Received remote ICE (${if (isCaller) "callee" else "caller"}): sdp=$sdp")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun listenForAnswer() {
        database.child("calls/$roomId/answer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    val type = snapshot.child("type").getValue(String::class.java)

                    if (sdp != null && type == "answer") {
                        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
                        peerConnection.setRemoteDescription(object : SdpObserver {
                            override fun onSetSuccess() {}
                            override fun onSetFailure(p0: String?) {}
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, answer)

                        Log.d("CALL_FLOW", "Caller: Answer received from Firebase")

                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun initializePeerConnectionFactory(context: Context) {
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initializationOptions)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .createPeerConnectionFactory()
    }

    fun listenForCalleeCandidates() {
        database.child("calls/$roomId/calleeCandidates")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val sdp = snapshot.child("candidate").getValue(String::class.java)
                    val sdpMid = snapshot.child("sdpMid").getValue(String::class.java)
                    val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java)

                    if (sdp != null && sdpMid != null && sdpMLineIndex != null) {
                        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                        peerConnection.addIceCandidate(candidate)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
package com.example.socialmediaproject.ui.videocall

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
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class VideoCallViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance("https://vector-mega-default-rtdb.asia-southeast1.firebasedatabase.app").reference

    val callStatus = MutableLiveData<String>()
    var isCaller: Boolean = false
    lateinit var peerConnectionFactory: PeerConnectionFactory
    lateinit var peerConnection: PeerConnection
    lateinit var localAudioTrack: AudioTrack
    lateinit var localAudioSource: AudioSource
    lateinit var localVideoTrack: VideoTrack
    lateinit var localVideoSource: VideoSource
    lateinit var videoCapturer: VideoCapturer
    lateinit var eglBase: EglBase
    lateinit var remoteRenderer: SurfaceViewRenderer
    var roomId: String = ""

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
            .setUsername("openrelayproject")
            .setPassword("openrelayproject")
            .createIceServer()
    )

    fun initFactoryAndTracks(context: Context, localRenderer: SurfaceViewRenderer, remoteRenderer: SurfaceViewRenderer) {
        eglBase = EglBase.create()
        this.remoteRenderer = remoteRenderer

        val options = PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()

        val audioConstraints = MediaConstraints()
        localAudioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", localAudioSource)

        videoCapturer = createVideoCapturer(context)
        val surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        localVideoSource = peerConnectionFactory.createVideoSource(false)
        videoCapturer.initialize(surfaceHelper, context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(640, 480, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("102", localVideoSource)
        localRenderer.init(eglBase.eglBaseContext, null)
        localRenderer.setMirror(true)
        localVideoTrack.addSink(localRenderer)

        remoteRenderer.init(eglBase.eglBaseContext, null)
    }

    fun createVideoCapturer(context: Context): VideoCapturer {
        val enumerator = Camera2Enumerator(context)
        val devices = enumerator.deviceNames
        for (device in devices) {
            if (enumerator.isFrontFacing(device)) {
                return enumerator.createCapturer(device, null)!!
            }
        }
        throw IllegalStateException("No front camera found")
    }

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

            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track()
                if (track is VideoTrack) {
                    track.setEnabled(true)
                    track.addSink(remoteRenderer)
                }
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddTrack(rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
            override fun onDataChannel(dc: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {}
        })!!

        peerConnection.addTrack(localAudioTrack)
        peerConnection.addTrack(localVideoTrack)
    }

    fun startCall() {
        val mediaConstraints = MediaConstraints()
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {}
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                }, sdp)
                val offer = mapOf("sdp" to sdp.description, "type" to sdp.type.canonicalForm())
                database.child("calls/$roomId/offer").setValue(offer)
            }

            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
            override fun onCreateFailure(p0: String?) {}
        }, mediaConstraints)
    }

    fun listenForOffer() {
        database.child("calls/$roomId/offer")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sdp = snapshot.child("sdp").getValue(String::class.java)
                    val type = snapshot.child("type").getValue(String::class.java)
                    if (sdp != null && type == "offer") {
                        val offer = SessionDescription(SessionDescription.Type.OFFER, sdp)
                        peerConnection.setRemoteDescription(object : SdpObserver {
                            override fun onSetSuccess() { createAnswer() }
                            override fun onSetFailure(p0: String?) {}
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {}
                        }, offer)
                    }
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
                val answer = mapOf("sdp" to sdp.description, "type" to sdp.type.canonicalForm())
                database.child("calls/$roomId/answer").setValue(answer)
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(p0: String?) {}
            override fun onCreateFailure(p0: String?) {}
        }, constraints)
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
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        val data = mapOf(
            "candidate" to candidate.sdp,
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex
        )
        val path = if (isCaller) "callerCandidates" else "calleeCandidates"
        database.child("calls/$roomId/$path").push().setValue(data)
    }

    fun listenForCallerCandidates() {
        database.child("calls/$roomId/callerCandidates")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                    val candidate = parseIceCandidate(snapshot)
                    if (candidate != null) peerConnection.addIceCandidate(candidate)
                }
                override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
                override fun onChildRemoved(p0: DataSnapshot) {}
                override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun listenForCalleeCandidates() {
        database.child("calls/$roomId/calleeCandidates")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, prev: String?) {
                    val candidate = parseIceCandidate(snapshot)
                    if (candidate != null) peerConnection.addIceCandidate(candidate)
                }
                override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
                override fun onChildRemoved(p0: DataSnapshot) {}
                override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun parseIceCandidate(snapshot: DataSnapshot): IceCandidate? {
        val sdp = snapshot.child("candidate").getValue(String::class.java)
        val sdpMid = snapshot.child("sdpMid").getValue(String::class.java)
        val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java)
        return if (sdp != null && sdpMid != null && sdpMLineIndex != null) {
            IceCandidate(sdpMid, sdpMLineIndex, sdp)
        } else null
    }

    fun endCall() {
        try {
            peerConnection.close()
            localAudioTrack.dispose()
            localAudioSource.dispose()
            localVideoTrack.dispose()
            localVideoSource.dispose()
            videoCapturer.stopCapture()
            videoCapturer.dispose()
            callStatus.postValue("ended")
            Log.d("VIDEO_CALL", "PeerConnection and tracks closed")
        } catch (e: Exception) {
            Log.e("VIDEO_CALL", "Error ending call: ${e.message}")
        }
    }
}
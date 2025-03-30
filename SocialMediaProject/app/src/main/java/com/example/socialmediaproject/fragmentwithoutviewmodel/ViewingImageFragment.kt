package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class ViewingImageFragment : Fragment() {

    private lateinit var closebutton: ImageView
    private lateinit var imageview: ImageView
    private var imageurl: String?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_viewing_image, container, false)
        closebutton=view.findViewById(R.id.btnClose)
        imageview=view.findViewById(R.id.imageViewFullScreen)
        imageurl=arguments?.getString("IMAGE_URL")
        imageurl?.let {
            Glide.with(this).load(it).into(imageview)
        }
        closebutton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.isEnabled=false
        bottomnavbar.visibility=View.INVISIBLE
        bottomnavbar.setOnTouchListener { _, _ -> true }
        return view
    }

    companion object {
        fun newInstance(imgurl: String): ViewingImageFragment {
            val fragment= ViewingImageFragment()
            val args=Bundle()
            args.putString("IMAGE_URL", imgurl)
            fragment.arguments=args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val bottomnavbar=requireActivity().findViewById<BottomNavigationView>(R.id.nav_view)
        bottomnavbar.isEnabled=true
        bottomnavbar.visibility=View.VISIBLE
        bottomnavbar.setOnTouchListener(null)
    }
}
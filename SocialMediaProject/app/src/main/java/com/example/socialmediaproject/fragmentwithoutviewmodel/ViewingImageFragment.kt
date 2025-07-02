package com.example.socialmediaproject.fragmentwithoutviewmodel

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.socialmediaproject.R
import com.example.socialmediaproject.activity.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class ViewingImageFragment : Fragment() {
    private lateinit var imageview: ImageView
    private var imageurl: String?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_viewing_image, container, false)
        imageview=view.findViewById(R.id.imageViewFullScreen)
        imageurl=arguments?.getString("IMAGE_URL")
        imageurl?.let {
            Glide.with(this).load(it).into(imageview)
        }
        (requireActivity() as MainActivity).hideNavigationWithBlur()
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
        (requireActivity() as MainActivity).showNavigationWithBlur()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).hideNavigationWithBlur()
    }
}
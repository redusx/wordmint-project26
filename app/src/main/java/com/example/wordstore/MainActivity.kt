package com.example.wordstore

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.wordstore.data.ResultWrapper
import com.example.wordstore.databinding.ActivityMainBinding
import com.example.wordstore.viewmodel.TranslationViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TranslationViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupListeners()
        observeViewModel()
    }
    
    private fun setupListeners() {
        binding.buttonTranslate.setOnClickListener {
            val inputText = binding.editTextInput.text.toString().trim()
            if (inputText.isNotEmpty()) {
                viewModel.translateText(inputText)
            } else {
                Toast.makeText(this, "Please enter text to translate", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.translationResult.observe(this) { result ->
            when (result) {
                is ResultWrapper.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonTranslate.isEnabled = false
                }
                is ResultWrapper.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonTranslate.isEnabled = true
                    
                    val translatedText = result.data.firstOrNull()?.translationText ?: ""
                    binding.textViewResult.text = translatedText
                }
                is ResultWrapper.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonTranslate.isEnabled = true
                    
                    val errorMessage = result.message
                    binding.textViewResult.text = "Error: $errorMessage"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
package com.fit3161.fit3162.mogo.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.User
import com.fit3161.fit3162.mogo.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    fun loadUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = supabase.from("users").select().decodeList<User>()
            _users.postValue(result)
        }
    }
}
package com.example.lession_equipo1.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lession_equipo1.model.UserProfile
import com.example.lession_equipo1.repository.AuthRepository
import com.example.lession_equipo1.repository.FirestoreRepository
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
    object PasswordResetSent : AuthState()
    object VerificationEmailSent : AuthState()
    object BodyDataNeeded : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: FirestoreRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state
    private val _profileUpdateState = MutableStateFlow<AuthState>(AuthState.Idle)
    val profileUpdateState: StateFlow<AuthState> = _profileUpdateState

    private val _notifications = MutableSharedFlow<String>()
    val notifications = _notifications.asSharedFlow()

    val currentUser = authRepository.currentUser

    fun signIn(context: Context) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val credentialManager = CredentialManager.create(context)
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(authRepository.getGoogleIdOption())
                    .build()

                val result = credentialManager.getCredential(context, request)
                handleSignInResult(result)
            } catch (e: GetCredentialException) {
                _state.value = AuthState.Error(e.message ?: "Error al obtener credenciales")
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                authRepository.signInWithEmail(email, pass)
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun registerWithEmail(
        email: String, 
        pass: String, 
        name: String
    ) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                authRepository.signUpWithEmail(email, pass, name)
                _state.value = AuthState.VerificationEmailSent
                _notifications.emit("Verifica tu identidad: Hemos enviado un enlace de confirmación a tu correo.")
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al registrarse")
            }
        }
    }

    fun updateBodyData(
        age: Int,
        weight: Double,
        height: Double,
        gender: String,
        sport: String
    ) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                authRepository.updateBodyData(age, weight, height, gender, sport)
                
                val user = authRepository.currentUser.value
                if (user != null) {
                    val profile = UserProfile(
                        displayName = user.name,
                        email = user.email,
                        photoUrl = user.photoUrl.orEmpty(),
                        age = age,
                        weight = weight,
                        height = height,
                        gender = gender,
                        sport = sport
                    )
                    firestoreRepository.createOrUpdateProfile(user.uid, profile)
                }
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al guardar datos")
            }
        }
    }

    fun updateProfile(
        name: String,
        age: Int,
        weight: Double,
        height: Double,
        gender: String,
        sport: String
    ) {
        viewModelScope.launch {
            _profileUpdateState.value = AuthState.Loading
            try {
                val user = authRepository.currentUser.value
                    ?: throw IllegalStateException("Inicia sesión para modificar tu perfil.")
                require(name.isNotBlank()) { "El nombre no puede estar vacío." }
                require(age in 1..120) { "Ingresa una edad entre 1 y 120 años." }
                require(weight in 1.0..500.0) { "Ingresa un peso válido en kg." }
                require(height in 1.0..300.0) { "Ingresa una altura válida en cm." }
                require(sport.isNotBlank()) { "Indica tu deporte o actividad principal." }

                val normalizedName = name.trim()
                val normalizedGender = gender.trim()
                val normalizedSport = sport.trim()

                firestoreRepository.createOrUpdateProfile(
                    uid = user.uid,
                    profile = UserProfile(
                        displayName = normalizedName,
                        email = user.email,
                        photoUrl = user.photoUrl.orEmpty(),
                        age = age,
                        weight = weight,
                        height = height,
                        gender = normalizedGender,
                        sport = normalizedSport
                    )
                )

                authRepository.updateProfile(
                    name = normalizedName,
                    age = age,
                    weight = weight,
                    height = height,
                    gender = normalizedGender,
                    sport = normalizedSport
                )
                _profileUpdateState.value = AuthState.Success
            } catch (e: Exception) {
                _profileUpdateState.value = AuthState.Error(
                    e.message ?: "No se pudo guardar el perfil."
                )
            }
        }
    }

    fun resetProfileUpdateState() {
        _profileUpdateState.value = AuthState.Idle
    }

    fun setProfileValidationError(message: String) {
        _profileUpdateState.value = AuthState.Error(message)
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                authRepository.sendPasswordResetEmail(email)
                _state.value = AuthState.PasswordResetSent
                _notifications.emit("Recuperación enviada: Revisa tu correo para cambiar la contraseña.")
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al enviar correo de recuperación")
            }
        }
    }

    fun confirmReset(code: String, newPass: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                authRepository.confirmPasswordReset(code, newPass)
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al restablecer contraseña")
            }
        }
    }

    fun loginAnonymously() {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                authRepository.signInAnonymously()
                _state.value = AuthState.Success
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al acceder como invitado")
            }
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        if ((credential is CustomCredential) &&
            (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            authRepository.signInWithGoogle(googleIdTokenCredential.idToken)
            _state.value = AuthState.Success
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _state.value = AuthState.Idle
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                authRepository.deleteAccount()
                _state.value = AuthState.Idle
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Error al eliminar cuenta")
            }
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }
}

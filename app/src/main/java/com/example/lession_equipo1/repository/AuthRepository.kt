package com.example.lession_equipo1.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.lession_equipo1.BuildConfig
import com.example.lession_equipo1.model.AppUser
import com.example.lession_equipo1.network.SupabaseClient
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AuthRepository(context: Context) {
    private val supabaseClient = SupabaseClient.client
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)
    private val prefs = context.getSharedPreferences("lession_user_data", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                val nameToDisplay = user.displayName ?: user.email?.substringBefore("@") ?: "Usuario"
                val localUser = loadLocalData(user.uid, nameToDisplay, user.email ?: "")
                _currentUser.value = localUser
            } else if (supabaseClient.auth.currentUserOrNull() == null) {
                _currentUser.value = null
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = supabaseClient.auth.currentUserOrNull()
                        if (user != null) {
                            val nameToDisplay = user.userMetadata?.get("full_name")?.jsonPrimitive?.content 
                                ?: user.email?.substringBefore("@") 
                                ?: "Usuario"
                            
                            val appUser = AppUser(
                                uid = user.id,
                                name = nameToDisplay,
                                email = user.email ?: "",
                                photoUrl = user.userMetadata?.get("avatar_url")?.jsonPrimitive?.content,
                                age = user.userMetadata?.get("age")?.jsonPrimitive?.content?.toIntOrNull() ?: prefs.getInt("age_${user.id}", 0),
                                weight = user.userMetadata?.get("weight")?.jsonPrimitive?.content?.toDoubleOrNull() ?: prefs.getFloat("weight_${user.id}", 0f).toDouble(),
                                height = user.userMetadata?.get("height")?.jsonPrimitive?.content?.toDoubleOrNull() ?: prefs.getFloat("height_${user.id}", 0f).toDouble(),
                                gender = user.userMetadata?.get("gender")?.jsonPrimitive?.content ?: prefs.getString("gender_${user.id}", "") ?: "",
                                sport = user.userMetadata?.get("sport")?.jsonPrimitive?.content ?: prefs.getString("sport_${user.id}", "") ?: ""
                            )
                            _currentUser.value = appUser
                            saveLocalData(appUser)
                        }
                    }
                    else -> if (firebaseAuth.currentUser == null) _currentUser.value = null
                }
            }
        }
    }

    private fun loadLocalData(uid: String, name: String, email: String): AppUser {
        return AppUser(
            uid = uid,
            name = prefs.getString("name_$uid", null)?.takeIf { it.isNotBlank() } ?: name,
            email = email,
            age = prefs.getInt("age_$uid", 0),
            weight = prefs.getFloat("weight_$uid", 0f).toDouble(),
            height = prefs.getFloat("height_$uid", 0f).toDouble(),
            gender = prefs.getString("gender_$uid", "") ?: "",
            sport = prefs.getString("sport_$uid", "") ?: ""
        )
    }

    private fun saveLocalData(user: AppUser) {
        prefs.edit().apply {
            putString("name_${user.uid}", user.name)
            putInt("age_${user.uid}", user.age)
            putFloat("weight_${user.uid}", user.weight.toFloat())
            putFloat("height_${user.uid}", user.height.toFloat())
            putString("gender_${user.uid}", user.gender)
            putString("sport_${user.uid}", user.sport)
            apply()
        }
    }

    suspend fun signInWithGoogle(token: String) {
        require(token.isNotBlank()) { "El token de Google es inválido." }

        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(token, null)
        firebaseAuth.signInWithCredential(credential).await()

        try {
            supabaseClient.auth.signInWith(IDToken) {
                idToken = token
                provider = Google
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun signInWithEmail(email: String, pass: String) {
        val normalizedEmail = email.trim()
        require(normalizedEmail.isNotBlank()) { "El email es obligatorio." }
        require(pass.isNotBlank()) { "La contraseña es obligatoria." }

        firebaseAuth.signInWithEmailAndPassword(normalizedEmail, pass).await()
        try {
            supabaseClient.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
                this.email = normalizedEmail
                this.password = pass
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun signInAnonymously() {
        try {
            firebaseAuth.signInAnonymously().await()
        } catch (e: Exception) {
            _currentUser.value = AppUser(uid = "demo_user_id", name = "Invitado", email = "demo@lession.app")
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, fullName: String) {
        val normalizedEmail = email.trim()
        val normalizedName = fullName.trim()
        require(normalizedEmail.isNotBlank()) { "El email es obligatorio." }
        require(pass.length >= 6) { "La contraseña debe tener al menos 6 caracteres." }
        require(normalizedName.isNotBlank()) { "El nombre es obligatorio." }

        val result = firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, pass).await()
        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest { displayName = normalizedName }
        result.user?.updateProfile(profileUpdates)?.await()
        try {
            result.user?.sendEmailVerification()?.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateBodyData(age: Int, weight: Double, height: Double, gender: String, sport: String) {
        try {
            supabaseClient.auth.updateUser {
                data = buildJsonObject {
                    put("age", age)
                    put("weight", weight)
                    put("height", height)
                    put("gender", gender)
                    put("sport", sport)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        _currentUser.value?.let {
            val updated = it.copy(age = age, weight = weight, height = height, gender = gender, sport = sport)
            _currentUser.value = updated
            saveLocalData(updated)
        }
    }

    suspend fun updateProfile(
        name: String,
        age: Int,
        weight: Double,
        height: Double,
        gender: String,
        sport: String
    ) {
        val currentUser = _currentUser.value
            ?: throw IllegalStateException("No hay un usuario autenticado.")

        firebaseAuth.currentUser?.let { firebaseUser ->
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = name
            }
            firebaseUser.updateProfile(profileUpdates).await()
        }

        val updatedUser = currentUser.copy(
            name = name,
            age = age,
            weight = weight,
            height = height,
            gender = gender,
            sport = sport
        )
        saveLocalData(updatedUser)
        _currentUser.value = updatedUser
    }

    suspend fun confirmPasswordReset(code: String, newPass: String) {
        // 1. Intentar en Firebase
        try {
            firebaseAuth.confirmPasswordReset(code, newPass).await()
        } catch (e: Exception) {
            // Si falla, es posible que el código sea de Supabase
            try {
                supabaseClient.auth.updateUser {
                    password = newPass
                }
            } catch (ex: Exception) {
                throw Exception("No se pudo restablecer la contraseña en ningún servicio.")
            }
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        supabaseClient.auth.signOut()
        _currentUser.value = null
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    suspend fun deleteAccount() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            prefs.edit().apply {
                remove("name_$uid"); remove("age_$uid"); remove("weight_$uid"); remove("height_$uid")
                remove("gender_$uid"); remove("sport_$uid")
                apply()
            }
        }
        firebaseAuth.currentUser?.delete()?.await()
        signOut()
    }

    fun getGoogleIdOption(): GetGoogleIdOption {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
    }
    
    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
        try { supabaseClient.auth.resetPasswordForEmail(email) } catch (e: Exception) {}
    }
}

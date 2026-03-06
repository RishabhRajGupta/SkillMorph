
package com.skillmorph.skillmorph.data.repository

import androidx.credentials.GetCredentialResponse
import com.skillmorph.skillmorph.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementation of the AuthRepository.
 * @param auth The FirebaseAuth instance.
 */
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUserFlow: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(authStateListener)
        awaitClose {
            auth.removeAuthStateListener(authStateListener)
        }
    }

    override suspend fun signInWithGoogle(result: GetCredentialResponse): Flow<Result<FirebaseUser>> = flow {
        try {
            // Use GoogleIdTokenCredential to safely parse the response
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = googleIdTokenCredential.idToken
            
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            emit(Result.success(authResult.user!!))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    override fun getLoggedInUser(): FirebaseUser? {
        return auth.currentUser
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}

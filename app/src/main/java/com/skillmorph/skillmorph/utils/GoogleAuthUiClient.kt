
package com.skillmorph.skillmorph.utils

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption

object GoogleAuthUiClient {

    private const val WEB_CLIENT_ID = "119333691668-1hug4q4e6231mond38sgl5jhgvjv1239.apps.googleusercontent.com"

    fun getGoogleIdOption(): GetGoogleIdOption {
        return GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Ensure we show ALL accounts on the first try
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false) // Disable auto-select to prevent "No Credential" errors when no default is set
            .build()
    }

    fun getCredentialRequest(googleIdOption: GetGoogleIdOption): GetCredentialRequest {
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            // This is key: it tells the system NOT to give up if a credential isn't "immediately" ready
            .setPreferImmediatelyAvailableCredentials(false)
            .build()
    }

    fun getCredentialManager(context: Context): CredentialManager {
        return CredentialManager.create(context)
    }
}

package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.IvoryBackgroundBottom
import com.example.ui.theme.MedGreen
import com.example.ui.theme.MedGreenLightSurface
import com.example.ui.theme.WarmBorder
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  val backgroundBrush = Brush.verticalGradient(listOf(IvoryBackground, IvoryBackgroundBottom))

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(backgroundBrush)
        .windowInsetsPadding(WindowInsets.safeDrawing),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier =
        Modifier.widthIn(max = 560.dp)
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 24.dp)
          .verticalScroll(rememberScrollState()),
      shape = RoundedCornerShape(28.dp),
      color = CreamSurface,
      border = BorderStroke(2.dp, WarmBorder),
      shadowElevation = 3.dp,
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
      ) {
        Box(
          modifier =
            Modifier.size(88.dp).clip(CircleShape).background(MedGreenLightSurface),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MedGreen,
            modifier = Modifier.size(48.dp),
          )
        }

        Text(
          text = stringResource(R.string.auth_title),
          style = MaterialTheme.typography.headlineLarge,
          color = InkPrimary,
          textAlign = TextAlign.Center,
        )

        Text(
          text = stringResource(R.string.auth_subtitle),
          style = MaterialTheme.typography.bodyLarge,
          color = InkSecondary,
          textAlign = TextAlign.Center,
        )

        if (errorMessage != null) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFDECEC),
            border = BorderStroke(1.5.dp, Color(0xFFD32F2F)),
          ) {
            Text(
              text = stringResource(R.string.auth_error_prefix, errorMessage.orEmpty()),
              style = MaterialTheme.typography.bodyLarge,
              color = Color(0xFFB71C1C),
              modifier = Modifier.padding(16.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
          onClick = {
            errorMessage = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isLoading = false },
            )
          },
          enabled = !isLoading,
          modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp).testTag("google_sign_in_button"),
          shape = RoundedCornerShape(22.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MedGreen,
              contentColor = Color.White,
            ),
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(32.dp),
              color = Color.White,
              strokeWidth = 3.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.AccountCircle,
              contentDescription = null,
              modifier = Modifier.size(34.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = stringResource(R.string.auth_google_button),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onUnauthenticated()
      return
    }
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  scope.launch {
    try {
      val cred = credentialManager.getCredential(context, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onAuthError("Google Sign-In 설정이 누락되었습니다.")
      return
    }
  val activityContext = context as? Activity
  if (activityContext == null) {
    onAuthError("화면을 다시 열고 시도해 주세요.")
    return
  }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  scope.launch {
    try {
      val cred = credentialManager.getCredential(activityContext, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onAuthError("로그인 정보를 확인할 수 없습니다.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "로그인 중 오류가 발생했습니다.")
    }
  }
}

fun signOutSafely(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

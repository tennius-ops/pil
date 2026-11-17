package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BloodSugarRepository
import com.example.ui.BloodSugarViewModel
import com.example.ui.DailyCheckViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BloodSugarScreen
import com.example.ui.screens.DailyCheckScreen
import com.example.ui.screens.signOutSafely
import com.example.ui.theme.CreamSurface
import com.example.ui.theme.CreamSurfaceVariant
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.IvoryBackground
import com.example.ui.theme.MedGreen
import com.example.ui.theme.MedGreenDark
import com.example.ui.theme.SeniorHealthTheme
import com.example.ui.theme.WarmBorder
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private enum class HomeTab {
  MED_AND_WATER,
  BLOOD_SUGAR,
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirebaseApp.getApps(this).ifEmpty { FirebaseApp.initializeApp(this) }
    enableEdgeToEdge()
    setContent { SeniorHealthTheme { AppNavigation() } }
  }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) { AuthenticatedHome(currentUserId = user.uid) }
  }
}

@Composable
private fun AuthenticatedHome(currentUserId: String) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val dailyViewModel: DailyCheckViewModel = viewModel()
  val dailyUiState by dailyViewModel.uiState.collectAsStateWithLifecycle()

  val bloodSugarViewModel: BloodSugarViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            BloodSugarViewModel(BloodSugarRepository(db), currentUserId)
          }
        },
    )
  val bloodSugarInputState by bloodSugarViewModel.inputState.collectAsStateWithLifecycle()
  val bloodSugarListState by bloodSugarViewModel.recordsState.collectAsStateWithLifecycle()

  var selectedTab by rememberSaveable { mutableStateOf(HomeTab.MED_AND_WATER) }

  LifecycleResumeEffect(Unit) {
    dailyViewModel.onResumeCheckDate()
    onPauseOrDispose {}
  }

  DisposableEffect(context, dailyViewModel) {
    val dateChangeReceiver =
      object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          dailyViewModel.onResumeCheckDate()
        }
      }
    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_DATE_CHANGED)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
      }
    ContextCompat.registerReceiver(
      context,
      dateChangeReceiver,
      filter,
      ContextCompat.RECEIVER_EXPORTED,
    )
    onDispose { context.unregisterReceiver(dateChangeReceiver) }
  }

  val invalidRangeMsg = stringResource(R.string.blood_sugar_invalid_range)
  val savedMsg = stringResource(R.string.blood_sugar_saved_msg)

  Column(
    modifier =
      Modifier.fillMaxSize().background(IvoryBackground).windowInsetsPadding(WindowInsets.safeDrawing),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Top Senior Tab Bar + Sign-Out
    Column(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleLarge,
          color = InkPrimary,
        )

        OutlinedButton(
          onClick = { signOutSafely(credentialManager, scope) },
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.5.dp, WarmBorder),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.heightIn(min = 50.dp).testTag("sign_out_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = null,
            tint = InkSecondary,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.auth_sign_out),
            style = MaterialTheme.typography.bodyLarge,
            color = InkSecondary,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        SeniorTabButton(
          label = stringResource(R.string.tab_med_water),
          icon = Icons.Filled.Medication,
          selected = selectedTab == HomeTab.MED_AND_WATER,
          testTag = "tab_med_water",
          onClick = { selectedTab = HomeTab.MED_AND_WATER },
          modifier = Modifier.weight(1f),
        )
        SeniorTabButton(
          label = stringResource(R.string.tab_blood_sugar),
          icon = Icons.Filled.Bloodtype,
          selected = selectedTab == HomeTab.BLOOD_SUGAR,
          testTag = "tab_blood_sugar",
          onClick = { selectedTab = HomeTab.BLOOD_SUGAR },
          modifier = Modifier.weight(1f),
        )
      }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (selectedTab) {
        HomeTab.MED_AND_WATER -> {
          DailyCheckScreen(
            uiState = dailyUiState,
            onToggleMedication = dailyViewModel::onToggleMedication,
            onAddWaterCup = dailyViewModel::onAddWaterCup,
            onRemoveWaterCup = dailyViewModel::onRemoveWaterCup,
            applySafeDrawingInsets = false,
          )
        }

        HomeTab.BLOOD_SUGAR -> {
          BloodSugarScreen(
            inputState = bloodSugarInputState,
            listState = bloodSugarListState,
            onValueChange = bloodSugarViewModel::onValueChange,
            onAdjustValue = bloodSugarViewModel::onAdjustValue,
            onTimingSelected = bloodSugarViewModel::onTimingSelected,
            onNoteChange = bloodSugarViewModel::onNoteChange,
            onSaveClick = {
              bloodSugarViewModel.saveRecord(
                invalidRangeMessage = invalidRangeMsg,
                savedSuccessMessage = savedMsg,
              )
            },
            onDeleteRecord = bloodSugarViewModel::deleteRecord,
          )
        }
      }
    }
  }
}

@Composable
private fun SeniorTabButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bg = if (selected) MedGreen else CreamSurface
  val border = if (selected) MedGreenDark else WarmBorder
  val contentColor = if (selected) Color.White else InkPrimary

  Surface(
    modifier =
      modifier
        .heightIn(min = 66.dp)
        .clip(RoundedCornerShape(20.dp))
        .clickable(role = Role.Tab, onClick = onClick)
        .testTag(testTag),
    shape = RoundedCornerShape(20.dp),
    color = bg,
    border = BorderStroke(2.dp, border),
    shadowElevation = if (selected) 3.dp else 1.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (selected) Color.White else MedGreen,
        modifier = Modifier.size(28.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = contentColor,
      )
    }
  }
}

package com.example.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.AccountScreen
import com.example.ui.AccountViewModel
import com.example.ui.AppViewModel
import com.example.ui.LocalActiveAccount
import com.example.ui.LoginScreen
import com.example.ui.RegistrationScreen
import com.example.ui.SplashScreen
import com.example.ui.TwoFactorAuthScreen
import com.example.ui.UserAccount
import com.example.ui.auth.AuthViewModel
import com.example.ui.botapi.BotFatherViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Type-safe central route constants for KuoteX Messenger navigation.
 */
object AppDestinations {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val REGISTER = "register"
    const val TWO_FACTOR = "2fa"
    const val CHAT_LIST = "chat_list"
    const val ARCHIVED_CHATS = "archived_chats"
    const val ARCHIVE_SETTINGS = "archive_settings"
    const val DISCOVER_BOTS = "discover_bots"
    const val CONTACTS = "contacts"
    const val CALLS = "calls"
    const val SETTINGS = "settings"
    const val SETTINGS_ACCOUNTS = "settings/accounts"
    const val SETTINGS_PROFILE = "settings/profile"
    const val SETTINGS_GENERAL = "settings/general"
    const val SETTINGS_STORAGE = "settings/storage"
    const val SETTINGS_THEMES = "settings/themes"
    const val SETTINGS_SECURITY = "settings/security"
    const val CHAT = "chat/{chatId}"
    const val CALL = "call/{chatId}?isVideo={isVideo}"
    const val SANDBOX = "sandbox/{botId}"
    const val BROADCAST = "broadcast"
    const val BOT_DASHBOARD = "dashboard/{botId}"
    const val MY_PROFILE = "my_profile"
}

/**
 * Dedicated Authentication Navigation Graph with isolated Koin AuthViewModel scope.
 */
@Composable
fun AuthNavGraph(
    rootNavController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = koinViewModel(),
    appViewModel: AppViewModel = koinViewModel(),
    accounts: List<UserAccount>,
    requires2FA: String?,
    isAddingAccount: Boolean,
    onAuthSuccess: (String) -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    DisposableEffect(rootNavController) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ ->
            focusManager.clearFocus()
        }
        rootNavController.addOnDestinationChangedListener(listener)
        onDispose {
            rootNavController.removeOnDestinationChangedListener(listener)
        }
    }

    NavHost(
        navController = rootNavController,
        startDestination = if (requires2FA != null) AppDestinations.TWO_FACTOR else AppDestinations.AUTH,
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
    ) {
        composable(AppDestinations.AUTH) {
            LoginScreen(
                accounts = accounts,
                viewModel = appViewModel,
                onNavigateToRegister = { rootNavController.navigate(AppDestinations.REGISTER) },
                forceManualLogin = isAddingAccount,
                onLoginSuccess = { userIdentifier ->
                    appViewModel.clearAddingAccount()
                    val accountId = accounts.find {
                        it.id == userIdentifier ||
                        it.username.equals(userIdentifier, ignoreCase = true) ||
                        it.username.equals("@$userIdentifier", ignoreCase = true) ||
                        it.displayName.equals(userIdentifier, ignoreCase = true) ||
                        it.phoneNumber == userIdentifier
                    }?.id ?: userIdentifier
                    appViewModel.switchAccount(accountId)
                    onAuthSuccess(accountId)
                }
            )
        }
        composable(AppDestinations.REGISTER) {
            RegistrationScreen(
                accounts = accounts,
                viewModel = appViewModel,
                onNavigateToLogin = { rootNavController.navigate(AppDestinations.AUTH) },
                onRegisterSuccess = { userIdentifier ->
                    appViewModel.clearAddingAccount()
                    val accountId = accounts.find {
                        it.id == userIdentifier ||
                        it.username.equals(userIdentifier, ignoreCase = true) ||
                        it.username.equals("@$userIdentifier", ignoreCase = true) ||
                        it.displayName.equals(userIdentifier, ignoreCase = true) ||
                        it.phoneNumber == userIdentifier
                    }?.id ?: userIdentifier
                    appViewModel.switchAccount(accountId)
                    onAuthSuccess(accountId)
                },
                checkPhoneExists = { phone -> authViewModel.checkPhoneExists(phone) }
            )
        }
        composable(AppDestinations.TWO_FACTOR) {
            TwoFactorAuthScreen(
                onVerify = { appViewModel.verify2FA(it) },
                onCancel = { appViewModel.cancel2FA() }
            )
        }
    }
}

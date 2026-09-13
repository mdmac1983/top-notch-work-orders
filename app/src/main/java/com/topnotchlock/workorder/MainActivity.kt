package com.topnotchlock.workorder

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.topnotchlock.workorder.ui.MainViewModel
import com.topnotchlock.workorder.ui.nav.Routes
import com.topnotchlock.workorder.ui.screens.HistoryScreen
import com.topnotchlock.workorder.ui.screens.HomeScreen
import com.topnotchlock.workorder.ui.screens.NewWorkOrderScreen
import com.topnotchlock.workorder.ui.screens.PdfViewerScreen
import com.topnotchlock.workorder.ui.screens.ReviewEditScreen
import com.topnotchlock.workorder.ui.screens.SettingsScreen
import com.topnotchlock.workorder.ui.screens.VendorEditScreen
import com.topnotchlock.workorder.ui.screens.VendorListScreen
import com.topnotchlock.workorder.ui.theme.TopNotchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            TopNotchTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    if (viewModel.hasPendingShare()) {
                        navController.navigate(Routes.NEW_WORK_ORDER)
                    }
                }

                NavHost(navController = navController, startDestination = Routes.HOME) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            onNewWorkOrder = { navController.navigate(Routes.NEW_WORK_ORDER) },
                            onVendors = { navController.navigate(Routes.VENDORS) },
                            onHistory = { navController.navigate(Routes.HISTORY) },
                            onSettings = { navController.navigate(Routes.SETTINGS) }
                        )
                    }
                    composable(Routes.NEW_WORK_ORDER) {
                        NewWorkOrderScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onExtracted = {
                                navController.navigate(Routes.REVIEW) {
                                    popUpTo(Routes.NEW_WORK_ORDER) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Routes.REVIEW) {
                        ReviewEditScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onDone = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            },
                            onOpenPdf = { file -> navController.navigate(Routes.pdfView(file.absolutePath)) }
                        )
                    }
                    composable(Routes.VENDORS) {
                        VendorListScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onEditVendor = { vendorId -> navController.navigate(Routes.vendorEdit(vendorId)) }
                        )
                    }
                    composable(
                        Routes.VENDOR_EDIT,
                        arguments = listOf(navArgument("vendorId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val vendorId = backStackEntry.arguments?.getString("vendorId") ?: ""
                        VendorEditScreen(
                            viewModel = viewModel,
                            vendorId = vendorId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.HISTORY) {
                        HistoryScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenPdf = { file -> navController.navigate(Routes.pdfView(file.absolutePath)) }
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                    composable(
                        Routes.PDF_VIEW,
                        arguments = listOf(navArgument("path") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                        PdfViewerScreen(
                            viewModel = viewModel,
                            filePath = Uri.decode(encodedPath),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    /** Captures content shared in via Android's Share sheet (Gmail, Outlook, screenshot apps, etc). */
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return
        val type = intent.type ?: return

        when {
            type == "text/plain" -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { viewModel.receivePendingShareText(it) }
            }
            type.startsWith("image/") -> {
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let { viewModel.receivePendingShareImage(it) }
            }
            type == "application/pdf" -> {
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let { viewModel.receivePendingSharePdf(it) }
            }
        }
    }
}

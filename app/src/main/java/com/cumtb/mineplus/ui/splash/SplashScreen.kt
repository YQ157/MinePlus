package com.cumtb.mineplus.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cumtb.mineplus.ui.theme.Dimens
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.large2)
        )
    }

    LaunchedEffect(Unit) {
        if (viewModel.shouldGoSchedule()) {
            onNavigateToSchedule()
        } else {
            onNavigateToLogin()
        }
    }
}
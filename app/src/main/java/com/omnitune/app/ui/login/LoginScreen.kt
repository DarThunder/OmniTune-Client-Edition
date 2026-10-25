package com.omnitune.app.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omnitune.app.ui.theme.Accent
import com.omnitune.app.ui.theme.OnAccent
import com.omnitune.app.ui.theme.SurfaceHigh
import com.omnitune.app.ui.theme.Titanium
import com.omnitune.app.ui.theme.TextPrimary

@Composable
fun LoginScreen(
    padding: PaddingValues,
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = hiltViewModel(),
) {
    val ui = vm.ui
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Accent,
        unfocusedBorderColor = Titanium.copy(alpha = 0.4f),
        focusedLabelColor = Accent,
        cursorColor = Accent,
        focusedContainerColor = SurfaceHigh,
        unfocusedContainerColor = SurfaceHigh,
    )

    Column(
        Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("OmniTune", style = MaterialTheme.typography.headlineMedium, color = Accent)
        Text("Tu música, tu servidor.", color = Titanium, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = ui.server,
            onValueChange = { v -> vm.update { it.copy(server = v) } },
            label = { Text("Servidor (Tailscale)") },
            placeholder = { Text("mi-pc.tu-tailnet.ts.net") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ui.username,
            onValueChange = { v -> vm.update { it.copy(username = v) } },
            label = { Text("Usuario") },
            singleLine = true,
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = ui.password,
            onValueChange = { v -> vm.update { it.copy(password = v) } },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(onClick = { vm.update { it.copy(showAdvanced = !it.showAdvanced) } }) {
            Text(if (ui.showAdvanced) "Ocultar avanzado" else "Avanzado: OSIRIS en otra URL", color = Titanium)
        }
        if (ui.showAdvanced) {
            OutlinedTextField(
                value = ui.auth,
                onValueChange = { v -> vm.update { it.copy(auth = v) } },
                label = { Text("URL de OSIRIS (vacío = mismo servidor)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }

        ui.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { vm.submit(onLoggedIn) },
            enabled = !ui.loading,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (ui.loading) CircularProgressIndicator(Modifier.size(22.dp), color = OnAccent, strokeWidth = 2.dp)
            else Text("Iniciar sesión", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(32.dp))
    }
}

package com.example.scriptbreak.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scriptbreak.R
import com.example.scriptbreak.ui.components.AppIcons
import com.example.scriptbreak.ui.components.AppTopBar
import com.example.scriptbreak.ui.components.DemoPrimaryButton
import com.example.scriptbreak.ui.theme.AppBackground
import com.example.scriptbreak.ui.theme.AppBlue
import com.example.scriptbreak.ui.theme.AppBlueLight
import com.example.scriptbreak.ui.theme.InkDark
import com.example.scriptbreak.ui.theme.MutedText
import com.example.scriptbreak.ui.theme.ScriptBreakTheme

@Composable
fun BankSelfScreen(
    onBack: () -> Unit,
    onBankAccount: (holderName: String, accountNumber: String, ifsc: String) -> Unit = { _, _, _ -> },
    onSelfTransfer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Bank transfer fields
    var accountNumber by remember { mutableStateOf("") }
    var confirmAccountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = stringResource(R.string.action_bank),
            onBack = onBack,
            showScriptBreakBadge = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Tabs: Bank Account vs To Self
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = AppBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AppBlue
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("To Bank A/c", fontSize = 15.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("To Self A/c", fontSize = 15.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (selectedTab == 0) {
                // Bank Account Form
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Recipient Bank Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )

                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it.filter(Char::isDigit).take(18) },
                            label = { Text("Bank Account Number") },
                            placeholder = { Text("e.g. 10023456789") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark,
                                cursorColor = AppBlue,
                                focusedBorderColor = AppBlue,
                                unfocusedBorderColor = Color(0xFFDDE2EA),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmAccountNumber,
                            onValueChange = { confirmAccountNumber = it.filter(Char::isDigit).take(18) },
                            label = { Text("Re-enter Account Number") },
                            placeholder = { Text("e.g. 10023456789") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark,
                                cursorColor = AppBlue,
                                focusedBorderColor = AppBlue,
                                unfocusedBorderColor = Color(0xFFDDE2EA),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = ifscCode,
                            onValueChange = { ifscCode = it.uppercase().take(11) },
                            label = { Text("IFSC Code") },
                            placeholder = { Text("e.g. DEMO0001234") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark,
                                cursorColor = AppBlue,
                                focusedBorderColor = AppBlue,
                                unfocusedBorderColor = Color(0xFFDDE2EA),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = recipientName,
                            onValueChange = { recipientName = it },
                            label = { Text("Account Holder Name") },
                            placeholder = { Text("As in passbook") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = InkDark,
                                unfocusedTextColor = InkDark,
                                cursorColor = AppBlue,
                                focusedBorderColor = AppBlue,
                                unfocusedBorderColor = Color(0xFFDDE2EA),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // To Self Form
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 0.5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Transfer to Self Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkDark
                        )

                        // From Account Card
                        Surface(
                            color = AppBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AppBlueLight)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.AccountBalance,
                                        contentDescription = null,
                                        tint = AppBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("From: Demo Bank", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                                    Text("A/c •••• 4821 (Primary)", fontSize = 13.sp, color = MutedText)
                                }
                            }
                        }

                        // To Account Card
                        Surface(
                            color = AppBackground,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AppBlueLight)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.AccountBalance,
                                        contentDescription = null,
                                        tint = AppBlue,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("To: Sample Bank", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                                    Text("A/c •••• 7319 (Secondary)", fontSize = 13.sp, color = MutedText)
                                }
                            }
                        }
                    }
                }
            }
        }

        DemoPrimaryButton(
            text = if (selectedTab == 0) "Proceed" else "Transfer to Self",
            onClick = {
                if (selectedTab == 0) {
                    if (accountNumber.isBlank() || recipientName.isBlank()) {
                        Toast.makeText(context, "Please fill in all bank details", Toast.LENGTH_SHORT).show()
                    } else if (accountNumber != confirmAccountNumber) {
                        Toast.makeText(context, "Account numbers do not match", Toast.LENGTH_SHORT).show()
                    } else {
                        // same amount screen, ScriptBreak check, ₹1 lakh limit and UPI PIN as a UPI payment
                        onBankAccount(recipientName, accountNumber, ifscCode)
                    }
                } else {
                    onSelfTransfer()
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BankSelfScreenPreview() {
    ScriptBreakTheme {
        BankSelfScreen(onBack = {})
    }
}

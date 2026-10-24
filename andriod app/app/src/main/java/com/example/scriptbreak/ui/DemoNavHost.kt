package com.example.scriptbreak.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.scriptbreak.ui.model.AmountUi
import com.example.scriptbreak.ui.model.CriticalUi
import com.example.scriptbreak.ui.model.FamilyAlertUi
import com.example.scriptbreak.ui.model.HomeUi
import com.example.scriptbreak.ui.model.PauseUi
import com.example.scriptbreak.ui.model.PayeeUi
import com.example.scriptbreak.ui.model.PersonUi
import com.example.scriptbreak.ui.model.SuccessUi
import com.example.scriptbreak.ui.model.TxnUi
import com.example.scriptbreak.ui.screens.AmountScreen
import com.example.scriptbreak.ui.screens.CriticalScreen
import com.example.scriptbreak.ui.screens.FamilyAlertScreen
import com.example.scriptbreak.ui.screens.HomeScreen
import com.example.scriptbreak.ui.screens.PauseScreen
import com.example.scriptbreak.ui.screens.PayToUpiScreen
import com.example.scriptbreak.ui.screens.PayeeScreen
import com.example.scriptbreak.ui.screens.PinScreen
import com.example.scriptbreak.ui.screens.SuccessScreen
import com.example.scriptbreak.ui.screens.UserPickerScreen
import com.example.scriptbreak.ui.screens.WaitingScreen
import kotlinx.coroutines.delay

enum class DemoScreen {
    UserPicker,
    Home,
    PayToUpi,
    Payee,
    Amount,
    Pause,
    Critical,
    Pin,
    Success,
    Waiting,
    FamilyAlert
}

@Composable
fun DemoNavHost(
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(DemoScreen.UserPicker) }

    // Demo user state
    var selectedUser by remember { mutableStateOf("Mr. Sharma") }
    var isOnCall by remember { mutableStateOf(true) }

    // Payee state
    var payeeName by remember { mutableStateOf("Ramesh K.") }
    var upiIdInput by remember { mutableStateOf("ramesh.k@demoupi") }
    var bankingName by remember { mutableStateOf("RAMESH KUMAR") }
    var historyLine by remember { mutableStateOf("New payee – never paid") }

    // Amount state
    var amountDigits by remember { mutableStateOf("20000") }
    var noteInput by remember { mutableStateOf("Verification deposit") }

    // Pause questions state
    var q1Answer by remember { mutableStateOf<Boolean?>(null) }
    var q2Answer by remember { mutableStateOf<Boolean?>(null) }
    var askedFamily by remember { mutableStateOf(false) }

    // Critical state
    var criticalHindi by remember { mutableStateOf(false) }
    var continueSecondsLeft by remember { mutableIntStateOf(5) }

    // Countdown timer for CriticalScreen
    LaunchedEffect(currentScreen, continueSecondsLeft) {
        if (currentScreen == DemoScreen.Critical && continueSecondsLeft > 0) {
            delay(1000L)
            continueSecondsLeft -= 1
        }
    }

    // PIN state
    var pinCount by remember { mutableIntStateOf(0) }

    // Sample people & transactions
    val samplePeople = listOf(
        PersonUi("ramesh", "Ramesh K.", "RK", "ramesh.k@demoupi", "#6B74D6"),
        PersonUi("anil", "Anil Mehta", "AM", "anil.m@demoupi", "#B07A3A"),
        PersonUi("sunita", "Sunita Iyer", "SI", "sunita.i@demoupi", "#D0679A"),
        PersonUi("suresh", "Suresh Verma", "SV", "suresh.v@demoupi", "#4F7FB0")
    )

    val sampleRecent = listOf(
        TxnUi("1", "Sunita Iyer", "Today 10:42 AM", "₹450"),
        TxnUi("2", "Amazon", "Yesterday, 6:12 AM", "₹1,299"),
        TxnUi("3", "Aarav Sharma", "16 Oct 2:00 PM", "₹800")
    )

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(targetState = currentScreen, label = "DemoNavAnimation") { screen ->
            when (screen) {
                DemoScreen.UserPicker -> {
                    UserPickerScreen(
                        onPick = { profileId ->
                            when (profileId) {
                                "rohit" -> {
                                    currentScreen = DemoScreen.FamilyAlert
                                }
                                "priya" -> {
                                    selectedUser = "Priya"
                                    isOnCall = false
                                    currentScreen = DemoScreen.Home
                                }
                                else -> {
                                    selectedUser = "Mr. Sharma"
                                    isOnCall = true
                                    currentScreen = DemoScreen.Home
                                }
                            }
                        }
                    )
                }

                DemoScreen.Home -> {
                    HomeScreen(
                        state = HomeUi(
                            userName = selectedUser,
                            onCall = isOnCall,
                            people = samplePeople,
                            recent = sampleRecent
                        ),
                        onPayUpiId = {
                            currentScreen = DemoScreen.PayToUpi
                        },
                        onPerson = { personId ->
                            val found = samplePeople.find { it.id == personId }
                            if (found != null) {
                                payeeName = found.name
                                upiIdInput = found.upiId
                                bankingName = if (found.id == "ramesh") "RAMESH KUMAR" else found.name.uppercase()
                                historyLine = if (found.id == "ramesh") "New payee – never paid" else "Paid 14 times before"
                            }
                            currentScreen = DemoScreen.Payee
                        },
                        onHistory = {
                            currentScreen = DemoScreen.UserPicker
                        },
                        onBalance = {
                            currentScreen = DemoScreen.PayToUpi
                        }
                    )
                }

                DemoScreen.PayToUpi -> {
                    PayToUpiScreen(
                        upiId = upiIdInput,
                        onUpiIdChange = { upiIdInput = it },
                        onPaste = { upiIdInput = "ramesh.k@demoupi" },
                        onContinue = {
                            payeeName = if (upiIdInput.contains("ramesh")) "Ramesh K." else "Payee (${upiIdInput.split("@").firstOrNull() ?: ""})"
                            bankingName = if (upiIdInput.contains("ramesh")) "RAMESH KUMAR" else "VERIFIED MERCHANT"
                            historyLine = "New payee – never paid"
                            currentScreen = DemoScreen.Payee
                        },
                        onBack = { currentScreen = DemoScreen.Home }
                    )
                }

                DemoScreen.Payee -> {
                    PayeeScreen(
                        state = PayeeUi(
                            name = payeeName,
                            upiId = upiIdInput,
                            bankingName = bankingName,
                            historyLine = historyLine
                        ),
                        onPay = {
                            currentScreen = DemoScreen.Amount
                        },
                        onBack = { currentScreen = DemoScreen.Home }
                    )
                }

                DemoScreen.Amount -> {
                    val formattedAmount = if (amountDigits.isBlank()) "0" else amountDigits
                    val nudgeText = if (amountDigits.isNotEmpty() && (amountDigits.toLongOrNull() ?: 0L) >= 10000L) {
                        "First payment to $payeeName. ₹$formattedAmount is 10× your usual payment. Check you know who this is."
                    } else null

                    AmountScreen(
                        state = AmountUi(
                            payeeName = payeeName,
                            amountText = amountDigits,
                            note = noteInput,
                            nudge = nudgeText,
                            payEnabled = amountDigits.isNotBlank() && (amountDigits.toLongOrNull() ?: 0L) > 0L
                        ),
                        onDigit = { digit ->
                            if (amountDigits.length < 7) {
                                amountDigits += digit.toString()
                            }
                        },
                        onDelete = {
                            if (amountDigits.isNotEmpty()) {
                                amountDigits = amountDigits.dropLast(1)
                            }
                        },
                        onNoteChange = { noteInput = it },
                        onPay = {
                            currentScreen = DemoScreen.Pause
                        },
                        onBack = { currentScreen = DemoScreen.Payee }
                    )
                }

                DemoScreen.Pause -> {
                    val formattedAmount = if (amountDigits.startsWith("₹")) amountDigits else "₹$amountDigits"
                    PauseScreen(
                        state = PauseUi(
                            amountText = formattedAmount,
                            payeeName = payeeName,
                            reasons = listOf(
                                "Active phone call detected during payment",
                                "New payee – never paid before",
                                "Amount is 10× higher than usual"
                            ),
                            q1 = q1Answer,
                            q2 = q2Answer,
                            contactName = "Rohit",
                            askedFamily = askedFamily
                        ),
                        onQ1 = { ans ->
                            q1Answer = ans
                        },
                        onQ2 = { ans ->
                            q2Answer = ans
                        },
                        onAskFamily = {
                            askedFamily = true
                        },
                        onNext = {
                            continueSecondsLeft = 5
                            currentScreen = DemoScreen.Critical
                        },
                        onBack = { currentScreen = DemoScreen.Amount }
                    )
                }

                DemoScreen.Critical -> {
                    val formattedAmount = if (amountDigits.startsWith("₹")) amountDigits else "₹$amountDigits"
                    CriticalScreen(
                        state = CriticalUi(
                            amountText = formattedAmount,
                            payeeName = payeeName,
                            isNewPayee = true,
                            reasons = listOf(
                                "Active phone call detected during this payment",
                                "New payee – never paid before",
                                "Amount is 10× higher than usual"
                            ),
                            contactName = "Rohit",
                            askedFamily = askedFamily,
                            continueSecondsLeft = continueSecondsLeft,
                            hindi = criticalHindi
                        ),
                        onWait = {
                            currentScreen = DemoScreen.Waiting
                        },
                        onAskFamily = {
                            askedFamily = true
                        },
                        onContinue = {
                            pinCount = 0
                            currentScreen = DemoScreen.Pin
                        },
                        onToggleLanguage = {
                            criticalHindi = !criticalHindi
                        },
                        onReadAloud = {}
                    )
                }

                DemoScreen.Pin -> {
                    val formattedAmount = if (amountDigits.startsWith("₹")) amountDigits else "₹$amountDigits"
                    PinScreen(
                        amountText = formattedAmount,
                        payeeName = payeeName,
                        pinDigits = pinCount,
                        onDigit = {
                            if (pinCount < 4) pinCount += 1
                        },
                        onDelete = {
                            if (pinCount > 0) pinCount -= 1
                        },
                        onSubmit = {
                            currentScreen = DemoScreen.Success
                        },
                        onBack = { currentScreen = DemoScreen.Critical }
                    )
                }

                DemoScreen.Success -> {
                    val formattedAmount = if (amountDigits.startsWith("₹")) amountDigits else "₹$amountDigits"
                    SuccessScreen(
                        state = SuccessUi(
                            amountText = formattedAmount,
                            payeeName = payeeName,
                            upiRef = "429810482910",
                            time = "Today, 10:45 AM",
                            showReportLine = true
                        ),
                        onDone = {
                            currentScreen = DemoScreen.Home
                        },
                        onReport = {
                            currentScreen = DemoScreen.UserPicker
                        }
                    )
                }

                DemoScreen.Waiting -> {
                    WaitingScreen(
                        onHome = { currentScreen = DemoScreen.Home }
                    )
                }

                DemoScreen.FamilyAlert -> {
                    val formattedAmount = if (amountDigits.startsWith("₹")) amountDigits else "₹$amountDigits"
                    FamilyAlertScreen(
                        state = FamilyAlertUi(
                            fromName = "Papa",
                            amountText = formattedAmount,
                            payeeName = payeeName,
                            reasons = listOf(
                                "Call detected during this payment",
                                "New payee – never paid before",
                                "Amount is 10× higher than usual"
                            )
                        ),
                        onCallNow = {
                            currentScreen = DemoScreen.UserPicker
                        },
                        onCalled = {
                            currentScreen = DemoScreen.UserPicker
                        }
                    )
                }
            }
        }
    }
}

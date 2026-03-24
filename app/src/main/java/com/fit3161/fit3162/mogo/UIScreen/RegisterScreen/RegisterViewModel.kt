package com.fit3161.fit3162.mogo.UIScreen.RegisterScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.AuthState
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RegisterUI(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val name: String = "",
    val gender: String = "",
    val countryCode: String = "",
    val phoneNumber: String = ""
)

data class Country(
    val name: String,
    val code: String
)

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _form = MutableStateFlow(RegisterUI())
    val form: StateFlow<RegisterUI> = _form.asStateFlow()

    val genderOptions = listOf(
        "Female",
        "Male",
        "Non-binary",
        "Genderqueer",
        "Agender",
        "Transgender",
        "Prefer not to say",
        "Other"
    )
    val countryOptions = listOf(
        // Australia & Oceania
        Country("Australia", "+61"),
        Country("New Zealand", "+64"),
        Country("Fiji", "+679"),
        Country("Papua New Guinea", "+675"),

        // North America
        Country("United States", "+1"),
        Country("Canada", "+1"),
        Country("Mexico", "+52"),

        // South America
        Country("Brazil", "+55"),
        Country("Argentina", "+54"),
        Country("Chile", "+56"),
        Country("Colombia", "+57"),
        Country("Peru", "+51"),

        // Europe
        Country("United Kingdom", "+44"),
        Country("Ireland", "+353"),
        Country("Germany", "+49"),
        Country("France", "+33"),
        Country("Italy", "+39"),
        Country("Spain", "+34"),
        Country("Netherlands", "+31"),
        Country("Sweden", "+46"),
        Country("Norway", "+47"),
        Country("Denmark", "+45"),
        Country("Finland", "+358"),
        Country("Poland", "+48"),
        Country("Switzerland", "+41"),
        Country("Austria", "+43"),

        // Asia
        Country("China", "+86"),
        Country("Japan", "+81"),
        Country("South Korea", "+82"),
        Country("India", "+91"),
        Country("Pakistan", "+92"),
        Country("Bangladesh", "+880"),
        Country("Sri Lanka", "+94"),
        Country("Nepal", "+977"),
        Country("Indonesia", "+62"),
        Country("Singapore", "+65"),
        Country("Malaysia", "+60"),
        Country("Thailand", "+66"),
        Country("Vietnam", "+84"),
        Country("Philippines", "+63"),

        // Middle East
        Country("United Arab Emirates", "+971"),
        Country("Saudi Arabia", "+966"),
        Country("Qatar", "+974"),
        Country("Kuwait", "+965"),
        Country("Israel", "+972"),
        Country("Turkey", "+90"),

        // Africa
        Country("South Africa", "+27"),
        Country("Nigeria", "+234"),
        Country("Kenya", "+254"),
        Country("Egypt", "+20"),
        Country("Morocco", "+212")
    )

    fun register(data: RegisterUI) {
        when {
            !isValidEmail(data.email) ->
                _state.value = AuthState.Error(
                    "Please use your @student.monash.edu or @monash.edu email."
                )
            data.password.length < 8 ->
                _state.value = AuthState.Error("Password must be at least 8 characters.")
            data.password != data.confirmPassword ->
                _state.value = AuthState.Error("Passwords do not match.")
            data.name.isBlank() ->
                _state.value = AuthState.Error("Please enter your name")
            data.countryCode.isBlank() ->
                _state.value = AuthState.Error("Please select a country code")
            countryOptions.none {it.code == data.countryCode} ->
                _state.value = AuthState.Error("Invalid country code")
            data.phoneNumber.length != 9 ->
                _state.value = AuthState.Error("Phone number must be 9 digits (e.g. 0xxxxxxxx")
            !isGenderValid(data.gender) ->
                _state.value = AuthState.Error("Please enter a valid gender, do not leave it blank.")

            else -> viewModelScope.launch {
                _state.value = AuthState.Loading

                val fullPhone = "$data.countryCode $data.phoneNumber"

                repo.register(
                    data.email.trim(),
                    data.password,
                    data.name,
                    fullPhone,
                    data.gender
                )
                    .onSuccess { _state.value = AuthState.AwaitingEmailConfirmation }
                    .onFailure {
                        _state.value = AuthState.Error(it.message ?: "Registration failed.")
                    }
            }
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }

    private fun isValidEmail(email: String): Boolean {
        val lower = email.trim().lowercase()
        return lower.endsWith("@student.monash.edu")
                || lower.endsWith("@monash.edu")
    }
    private fun isGenderValid(gender: String): Boolean {
        return gender in this.genderOptions ||
                (gender.startsWith("Other: ") && gender.removePrefix("Other: ").isNotBlank())
    }

    fun onEmailChange(value: String) {
        _form.value = _form.value.copy(email = value)
    }

    fun onNameChange(value: String) {
        _form.value = _form.value.copy(name = value)
    }

    fun onPasswordChange(value: String) {
        _form.value = _form.value.copy(password = value)
    }

    fun onConfirmPasswordChange(value: String) {
        _form.value = _form.value.copy(confirmPassword = value)
    }

    fun onGenderChange(value: String) {
        _form.value = _form.value.copy(gender = value)
    }

    fun onCountrySelected(code: String) {
        _form.value = _form.value.copy(countryCode = code)
    }

    fun onPhoneChange(value: String) {
        if (value.all { it.isDigit() } && value.length <= 9) {
            _form.value = _form.value.copy(phoneNumber = value)
        }
    }
}

class RegisterViewModelFactory(
    private val repo: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

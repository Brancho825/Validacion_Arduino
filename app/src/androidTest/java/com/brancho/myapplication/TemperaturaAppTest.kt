package com.brancho.myapplication

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemperaturaAppTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun TC01_verificarTemperaturaNormal_22Grados() {
        // Simular recepción de "22"
        activityRule.scenario.onActivity { activity ->
            activity.procesarDatoRecibido("22.0")
        }

        // REQ-01: Visualización con formato "XX°C"
        onView(withId(R.id.txtTemperatura)).check(matches(withText("22.0°C")))

        // REQ-02: Fondo VERDE (#00FF00)
        activityRule.scenario.onActivity { activity ->
            val background = activity.mainLayout.background as ColorDrawable
            assertEquals(Color.parseColor("#00FF00"), background.color)
        }
    }

    @Test
    fun TC02_verificarAlertaPorCalorExtremo_35Grados() {
        // Simular recepción de "35" (Valor límite)
        activityRule.scenario.onActivity { activity ->
            activity.procesarDatoRecibido("35.0")
        }

        // REQ-01: Visualización
        onView(withId(R.id.txtTemperatura)).check(matches(withText("35.0°C")))

        // REQ-03: Fondo ROJO (#FF0000)
        activityRule.scenario.onActivity { activity ->
            val background = activity.mainLayout.background as ColorDrawable
            assertEquals(Color.parseColor("#FF0000"), background.color)
        }

        // REQ-03: Mensaje de alerta
        onView(withId(R.id.txtMensajeAlerta)).check(matches(withText("¡ALERTA: CALOR EXTREMO!")))
    }

    @Test
    fun TC03_verificarManejoDeError_CadenaVacia() {
        // Simular recepción de cadena vacía
        activityRule.scenario.onActivity { activity ->
            activity.procesarDatoRecibido("")
        }

        // REQ-04: Fondo AMARILLO (#FFFF00)
        activityRule.scenario.onActivity { activity ->
            val background = activity.mainLayout.background as ColorDrawable
            assertEquals(Color.parseColor("#FFFF00"), background.color)
        }

        // REQ-04: Mensaje de error
        onView(withId(R.id.txtMensajeAlerta)).check(matches(withText("Error de lectura")))
    }

    @Test
    fun TC04_verificarManejoDeError_DatoCorrupto() {
        // Simular recepción de "err"
        activityRule.scenario.onActivity { activity ->
            activity.procesarDatoRecibido("err")
        }

        // REQ-04: Fondo AMARILLO (#FFFF00)
        activityRule.scenario.onActivity { activity ->
            val background = activity.mainLayout.background as ColorDrawable
            assertEquals(Color.parseColor("#FFFF00"), background.color)
        }

        // REQ-04: Mensaje de error
        onView(withId(R.id.txtMensajeAlerta)).check(matches(withText("Error de lectura")))
    }
}

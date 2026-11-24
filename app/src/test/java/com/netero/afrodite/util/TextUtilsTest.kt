package com.netero.afrodite.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TextUtilsTest {

    @Test
    fun `normalize remove acentos, espacos e maiusculas`() {
        assertEquals("camera", TextUtils.normalize("  Câmera "))
        assertEquals("configuracoes", TextUtils.normalize("Configurações"))
    }

    @Test
    fun `sectionLetter agrupa numeros e simbolos em cerquilha`() {
        assertEquals('#', TextUtils.sectionLetter("9GAG"))
        assertEquals('#', TextUtils.sectionLetter(""))
        assertEquals('I', TextUtils.sectionLetter("Ícone"))
        assertEquals('C', TextUtils.sectionLetter("çaixa"))
    }

    @Test
    fun `filter coloca primeiro quem comeca com a busca`() {
        val apps = listOf("Fotos", "Galeria", "Google Fotos", "Meus fotógrafos")
        val result = TextUtils.filter(apps, "fot") { TextUtils.normalize(it) }
        assertEquals(listOf("Fotos", "Google Fotos", "Meus fotógrafos"), result)
    }

    @Test
    fun `filter com busca vazia devolve tudo`() {
        val apps = listOf("A", "B")
        assertEquals(apps, TextUtils.filter(apps, "  ") { it })
    }
}

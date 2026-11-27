package com.example

import com.example.data.ChurchMemberDetail
import com.example.data.MemberExportUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberCvExportTest {

    @Test
    fun `suggestedMemberCvFileName formats member name cleanly for PDF`() {
        val member = ChurchMemberDetail(
            id = "member_cv_001",
            fullName = "Alexius bin Madius Toburon"
        )
        val fileName = MemberExportUtils.suggestedMemberCvFileName(member)
        assertTrue(fileName.startsWith("Alexius_bin_Madius_Toburon_Maklumat_Ahli_"))
        assertTrue(fileName.endsWith(".pdf"))
    }

    @Test
    fun `suggestedMemberCvFileName uses fallback when fullName is blank`() {
        val sparseMember = ChurchMemberDetail(fullName = "   ")
        val fileName = MemberExportUtils.suggestedMemberCvFileName(sparseMember)
        assertTrue(fileName.startsWith("Ahli_SIB_Maklumat_Ahli_"))
        assertEquals("pdf", fileName.substringAfterLast('.'))
    }
}

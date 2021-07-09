package ch.loewenfels.issuetrackingsync.controller

import ch.loewenfels.issuetrackingsync.app.SyncApplicationProperties
import ch.loewenfels.issuetrackingsync.notification.CsvProtocol
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintStream
import javax.servlet.http.HttpServletResponse

@Controller
class CsvController(
    private val properties: SyncApplicationProperties
) {

    @GetMapping("/protocolCsv")
    fun downloadCsv(response: HttpServletResponse) {
        val list = properties.notificationChannels.filterIsInstance<CsvProtocol>()
        if (list.isNotEmpty()) {
            val file = list.first().file
            response.contentType = "text/csv"
            response.setHeader(
                "Content-Disposition", "attachment; filename=\"protocol.csv\""
            )
            try {
                FileInputStream(file).use { IOUtils.copy(it, response.outputStream) }
            } catch (e: IOException) {
                e.printStackTrace(PrintStream(response.outputStream))
            }
            response.outputStream.close()
        }
    }
}

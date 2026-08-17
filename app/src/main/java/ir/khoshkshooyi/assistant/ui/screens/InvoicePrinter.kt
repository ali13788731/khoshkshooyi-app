package ir.khoshkshooyi.assistant.ui.screens

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import ir.khoshkshooyi.assistant.data.Order
import ir.khoshkshooyi.assistant.data.Shop
import ir.khoshkshooyi.assistant.toman
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the invoice as a single PDF page and hands it to Android's native print
 * dialog (Print to PDF, or any printer the user has set up) — no WebView involved.
 */
object InvoicePrinter {

    fun print(context: Context, shop: Shop?, order: Order) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "فاکتور #${order.invoiceNo}"
        printManager.print(jobName, InvoiceDocumentAdapter(shop, order), PrintAttributes.Builder().build())
    }

    private class InvoiceDocumentAdapter(private val shop: Shop?, private val order: Order) : PrintDocumentAdapter() {
        private var pdfDocument: PdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            pdfDocument = PdfDocument()
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("invoice_${order.invoiceNo}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val doc = pdfDocument ?: PdfDocument().also { pdfDocument = it }
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 at 72dpi
            val page = doc.startPage(pageInfo)
            drawInvoice(page.canvas)
            doc.finishPage(page)
            try {
                FileOutputStream(destination.fileDescriptor).use { out -> doc.writeTo(out) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
                pdfDocument = null
            }
        }

        private fun drawInvoice(canvas: android.graphics.Canvas) {
            val marginX = 48f
            var y = 60f
            val title = Paint().apply { textSize = 20f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
            val label = Paint().apply { textSize = 11f; color = 0xFF4B6167.toInt(); textAlign = Paint.Align.RIGHT }
            val value = Paint().apply { textSize = 12f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
            val line = Paint().apply { textSize = 12f; textAlign = Paint.Align.RIGHT }
            val small = Paint().apply { textSize = 10f; color = 0xFF4B6167.toInt(); textAlign = Paint.Align.RIGHT }
            val pageWidth = 595f
            val right = pageWidth - marginX

            canvas.drawText(shop?.name?.ifBlank { "خشکشویی" } ?: "خشکشویی", right, y, title)
            y += 18f
            canvas.drawText("فاکتور رسمی", right, y, small)
            y += 30f

            canvas.drawText("شماره فاکتور: #${order.invoiceNo}", right, y, line); y += 18f
            canvas.drawText("تاریخ: " + SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(order.date)), right, y, line); y += 18f
            val customerLine = "مشتری: " + order.customerName + if (order.customerNickname.isNotBlank()) " (${order.customerNickname})" else ""
            canvas.drawText(customerLine, right, y, line); y += 26f

            order.items.forEach { it2 ->
                val head = "${it2.type} × ${it2.count}"
                val amount = toman(it2.price * it2.count)
                canvas.drawText(head, right, y, value)
                val headWidth = value.measureText(head)
                canvas.drawText(amount, right - headWidth - 20f, y, value)
                y += 16f
                if (it2.services.isNotEmpty()) {
                    canvas.drawText(it2.services.joinToString(" · "), right, y, small)
                    y += 14f
                }
                if (it2.description.isNotBlank()) {
                    canvas.drawText(it2.description, right, y, small)
                    y += 14f
                }
                y += 6f
            }

            y += 10f
            canvas.drawText("جمع اقلام: ${toman(order.itemsTotal)}", right, y, line); y += 18f
            if (order.previousDebtLine > 0) {
                canvas.drawText("بدهی قبلی: ${toman(order.previousDebtLine)}", right, y, line); y += 18f
            }
            y += 6f
            val totalPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.RIGHT }
            canvas.drawText("قابل پرداخت: ${toman(order.total)}", right, y, totalPaint)
            y += 22f
            canvas.drawText(if (order.paymentStatus == "paid") "پرداخت شده" else "بدهکار", right, y, line)
            y += 22f

            if (order.notes.isNotBlank()) {
                canvas.drawText("یادداشت: ${order.notes}", right, y, small)
                y += 18f
            }

            canvas.drawText("با تشکر از اعتماد شما", right, y + 20f, small)
        }
    }
}

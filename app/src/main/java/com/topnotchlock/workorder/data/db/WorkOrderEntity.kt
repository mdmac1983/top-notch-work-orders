package com.topnotchlock.workorder.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.topnotchlock.workorder.data.WorkOrder

@Entity(tableName = "work_orders")
data class WorkOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val woNumber: String,
    val date: String,
    val companyName: String = "",
    val companyPhone: String = "",
    val siteName: String,
    val siteId: String,
    val address: String,
    val contact: String,
    val phone: String,
    val arriveBy: String,
    val completeBy: String,
    val problem: String,
    val vendorId: String,
    val vendorName: String,
    val createdAtMillis: Long,
    val pdfPath: String
) {
    fun toWorkOrder(): WorkOrder = WorkOrder(
        woNumber = woNumber,
        date = date,
        companyName = companyName,
        companyPhone = companyPhone,
        siteName = siteName,
        siteId = siteId,
        address = address,
        contact = contact,
        phone = phone,
        arriveBy = arriveBy,
        completeBy = completeBy,
        problem = problem,
        vendorId = vendorId,
        vendorName = vendorName
    )

    companion object {
        fun fromWorkOrder(wo: WorkOrder, createdAtMillis: Long, pdfPath: String): WorkOrderEntity =
            WorkOrderEntity(
                woNumber = wo.woNumber,
                date = wo.date,
                companyName = wo.companyName,
                companyPhone = wo.companyPhone,
                siteName = wo.siteName,
                siteId = wo.siteId,
                address = wo.address,
                contact = wo.contact,
                phone = wo.phone,
                arriveBy = wo.arriveBy,
                completeBy = wo.completeBy,
                problem = wo.problem,
                vendorId = wo.vendorId,
                vendorName = wo.vendorName,
                createdAtMillis = createdAtMillis,
                pdfPath = pdfPath
            )
    }
}

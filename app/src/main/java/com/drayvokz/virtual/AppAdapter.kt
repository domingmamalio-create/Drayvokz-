```kotlin
package com.drayvokz.virtual

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val items: MutableList<AppInfo>,
    private val onLaunch: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.imgIcon)
        val label: TextView = v.findViewById(R.id.txtLabel)
        val pkg: TextView = v.findViewById(R.id.txtPkg)
        val launch: Button = v.findViewById(R.id.btnLaunch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.pkg.text = item.packageName
        holder.icon.setImageDrawable(item.icon ?: holder.itemView.context.getDrawable(android.R.drawable.sym_def_app_icon))
        holder.launch.setOnClickListener { onLaunch(item) }
    }

    override fun getItemCount() = items.size

    fun replace(newItems: List<AppInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
```

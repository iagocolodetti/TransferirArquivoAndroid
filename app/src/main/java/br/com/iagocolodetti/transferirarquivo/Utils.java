/*
 * Copyright (C) 2019 Iago Colodetti
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package br.com.iagocolodetti.transferirarquivo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.ArrayAdapter;

/**
 *
 * @author iagocolodetti
 */
public class Utils {

    @SuppressLint("DefaultLocale")
    public static String bytesTo(long bytes, String bytesTo) {
        String size = "";
            switch (bytesTo) {
                case "B":
                    size = String.valueOf(bytes);
                    break;
                case "KB":
                    size = String.valueOf(bytes / 1024);
                    break;
                case "MB":
                    size = String.valueOf(bytes / 1024 / 1024);
                    break;
                case "GB":
                    size = String.format("%.2f", (double) bytes / 1024 / 1024 / 1024);
                    break;
                case "TB":
                    size = String.format("%.2f", (double) bytes / 1024 / 1024 / 1024 / 1024);
                    break;
            }
        return size;
    }

    public static ArrayAdapter<String> getSpinnerAdapter(Context context) {
        String[] spinnerItems = new String[]{"B", "KB", "MB", "GB", "TB"};
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(context, R.layout.spinner_textstyle, spinnerItems);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return dataAdapter;
    }
}

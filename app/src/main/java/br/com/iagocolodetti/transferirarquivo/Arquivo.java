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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 *
 * @author iagocolodetti
 */
public class Arquivo {

    private ContentResolver contentResolver;
    private Uri uri;
    private String name;
    private long size;

    public Arquivo(Context context, Uri uri) throws FileNotFoundException {
        contentResolver = context.getContentResolver();
        this.uri = uri;
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    size = Long.parseLong(cursor.getString(sizeIndex));
                } else {
                    throw new FileNotFoundException(context.getString(R.string.erro_arquivo_tamanho));
                }
            }
            if (name == null || name.isEmpty()) throw new FileNotFoundException(context.getString(R.string.erro_arquivo_nome));
        } catch (FileNotFoundException ex) {
            throw new FileNotFoundException(context.getString(R.string.erro_arquivo_nao_encontrado));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public InputStream getInputStream() throws FileNotFoundException {
        return contentResolver.openInputStream(uri);
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    @Override
    public boolean equals(Object object) {
        return getName().equals(((Arquivo)object).getName())
                && getSize() == ((Arquivo)object).getSize();
    }
}

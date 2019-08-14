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

import java.io.File;
import java.io.Serializable;

/**
 *
 * @author iagocolodetti
 */
public class Arquivo implements Serializable {

    private static final long serialVersionUID = 5925451328785184659L;

    private File arquivo;

    public Arquivo() {

    }

    public Arquivo(File arquivo) {
        this.arquivo = arquivo;
    }

    public void setArquivo(File arquivo) {
        this.arquivo = arquivo;
    }

    public File getArquivo() {
        return arquivo;
    }

    @Override
    public boolean equals(Object object) {
        return getArquivo().equals(((Arquivo)object).getArquivo());
    }
}

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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 *
 * @author iagocolodetti
 */
public class AddArquivosActivity extends AppCompatActivity {

    private ArrayList<Arquivo> arquivos = null;
    private long tamanhoTotal = 0;
    private int selecionadoParaRemover = -1;

    private EditText addEtTamanhoTotal;
    private Spinner addSprTamanhoTotal;
    private ListView addLvArquivos;
    private Button addBtRemover;

    // <editor-fold defaultstate="collapsed" desc="Métodos">
    private void exibirMensagem(final String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    private void removerArquivo() {
        if (selecionadoParaRemover != -1) {
            tamanhoTotal -= arquivos.get(selecionadoParaRemover).getSize();
            arquivos.remove(selecionadoParaRemover);
            addBtRemover.setText(getString(R.string.add_bt_remover));
            atualizarLvArquivos();
            addEtTamanhoTotal.setText(Utils.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
            selecionadoParaRemover = -1;
        }
    }

    private void removerTodosArquivos() {
        arquivos.clear();
        atualizarLvArquivos();
        tamanhoTotal = 0;
        addEtTamanhoTotal.setText("");
    }

    private void atualizarLvArquivos() {
        final String tipo = addSprTamanhoTotal.getSelectedItem().toString();
        ArrayAdapter<Arquivo> adapter = new ArrayAdapter<Arquivo>(this, R.layout.custom_listview, arquivos) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                Spanned texto = Html.fromHtml("<normal><font color=\"#FF0000\">[" + position + "]</font><font color=\"#FBFBFB\"> " + arquivos.get(position).getName() + "</font></normal>"
                        + "<br><small><font color=\"#C7E4E6\">Tamanho: " + Utils.calcularTamanho(tipo, arquivos.get(position).getSize()) + " " + tipo + "</font>", Html.FROM_HTML_MODE_LEGACY);
                text1.setText(texto);
                return view;
            }
        };
        addLvArquivos.setAdapter(adapter);
    }
    // </editor-fold>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_arquivos);

        Button addBtSelecionar = findViewById(R.id.addBtSelecionar);
        addLvArquivos = findViewById(R.id.addLvArquivos);
        addBtRemover = findViewById(R.id.addBtRemover);
        Button addBtRemoverTodos = findViewById(R.id.addBtRemoverTodos);
        addEtTamanhoTotal = findViewById(R.id.addEtTamanhoTotal);
        addSprTamanhoTotal = findViewById(R.id.addSprTamanhoTotal);
        Button addBtConfirmar = findViewById(R.id.addBtConfirmar);
        Button addBtCancelar = findViewById(R.id.addBtCancelar);

        addEtTamanhoTotal.setInputType(InputType.TYPE_NULL);

        arquivos = new ArrayList<>(ArquivoHelper.getArquivos());
        tamanhoTotal = ArquivoHelper.getTamanhoTotal();
        ArquivoHelper.clear();

        addBtSelecionar.setOnClickListener(view -> {
            Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("*/*");
            chooseFile.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            chooseFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            chooseFile.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            selecionarArquivos.launch(Intent.createChooser(chooseFile, getString(R.string.titulo_selecionar_arquivos)));
        });

        addLvArquivos.setOnItemClickListener((parent, view, position, id) -> {
            if (selecionadoParaRemover != position) {
                selecionadoParaRemover = position;
                addBtRemover.setText(getString(R.string.add_bt_remover_com_id, selecionadoParaRemover));
            } else {
                selecionadoParaRemover = -1;
                addBtRemover.setText(getString(R.string.add_bt_remover));
            }
        });

        addBtRemoverTodos.setOnClickListener(view -> removerTodosArquivos());

        addBtRemover.setOnClickListener(view -> removerArquivo());

        addSprTamanhoTotal.setAdapter(Utils.getSpinnerAdapter(this));
        addSprTamanhoTotal.setSelection(0);
        addSprTamanhoTotal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (arquivos != null && !arquivos.isEmpty()) {
                    atualizarLvArquivos();
                    addEtTamanhoTotal.setText(Utils.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        addBtCancelar.setOnClickListener(view -> finish());

        addBtConfirmar.setOnClickListener(view -> {
            if (arquivos.size() > 1) {
                ArquivoHelper.setArquivos(arquivos);
                ArquivoHelper.setTamanhoTotal(tamanhoTotal);
                setResult(RESULT_OK);
                finish();
            } else {
                exibirMensagem(getString(R.string.erro_confirmar_arquivos));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arquivos != null && !arquivos.isEmpty()) {
            atualizarLvArquivos();
            addEtTamanhoTotal.setText(Utils.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
        }
    }

    ActivityResultLauncher<Intent> selecionarArquivos = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent != null) {
                    try {
                        Uri uri;
                        int erros = 0;
                        if (intent.getClipData() != null) {
                            for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                                uri = intent.getClipData().getItemAt(i).getUri();
                                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                Arquivo arquivo = new Arquivo(this, uri);
                                if (!arquivos.contains(arquivo)) {
                                    arquivos.add(arquivo);
                                    tamanhoTotal += arquivo.getSize();
                                } else {
                                    erros++;
                                }
                            }
                            atualizarLvArquivos();
                            addEtTamanhoTotal.setText(Utils.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
                            if (erros > 0) {
                                exibirMensagem(getResources().getQuantityString(R.plurals.erro_selecionar_arquivos_ja_adicionados, erros));
                            }
                        } else {
                            uri = intent.getData();
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            Arquivo arquivo = new Arquivo(this, uri);
                            if (!arquivos.contains(arquivo)) {
                                arquivos.add(arquivo);
                                atualizarLvArquivos();
                                tamanhoTotal += arquivo.getSize();
                                addEtTamanhoTotal.setText(Utils.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
                            } else {
                                exibirMensagem(getString(R.string.erro_selecionar_arquivos_ja_adicionado));
                            }
                        }
                    } catch (FileNotFoundException ex) {
                        exibirMensagem(getString(R.string.erro_selecionar_arquivos));
                    }
                }
            }
        }
    );
}

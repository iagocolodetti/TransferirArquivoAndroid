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

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
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

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.util.ArrayList;

/**
 *
 * @author iagocolodetti
 */
public class AddArquivosActivity extends AppCompatActivity {

    private final int REQUEST_CODE_SELECIONAR_ARQUIVO = 1000;

    private File arquivo = null;
    private ArrayList<Arquivo> arquivos = null;
    private long tamanhoTotal = 0;
    private int selecionadoParaRemover = -1;

    private EditText addEtArquivo, addEtTamanho, addEtTamanhoTotal;
    private Spinner addSprTamanho, addSprTamanhoTotal;
    private ListView addLvArquivos;
    private Button addBtRemover;

    // <editor-fold defaultstate="collapsed" desc="Métodos">
    private void exibirMensagem(final String mensagem) {
        Toast.makeText(AddArquivosActivity.this, mensagem, Toast.LENGTH_SHORT).show();
    }

    private void adicionarArquivo() {
        if (arquivo != null) {
            Arquivo _arquivo = new Arquivo(arquivo);
            if (!arquivos.contains(_arquivo)) {
                arquivos.add(_arquivo);
                atualizarLvArquivos();
                tamanhoTotal += arquivo.length();
                addEtArquivo.setText("");
                addEtTamanho.setText("");
                addEtTamanhoTotal.setText(Util.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
                arquivo = null;
            } else {
                exibirMensagem("Erro: Esse arquivo já foi adicionado à lista.");
            }
        }
    }

    private void removerArquivo() {
        if (selecionadoParaRemover != -1) {
            tamanhoTotal -= arquivos.get(selecionadoParaRemover).getArquivo().length();
            arquivos.remove(selecionadoParaRemover);
            addBtRemover.setText("Remover");
            atualizarLvArquivos();
            addEtTamanhoTotal.setText(Util.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
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
        ArrayAdapter<Arquivo> adapter = new ArrayAdapter<Arquivo>(AddArquivosActivity.this, R.layout.custom_listview, arquivos) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                Spanned texto = Html.fromHtml("<normal><font color=\"#FF0000\">[" + position + "]</font><font color=\"#FBFBFB\"> " + arquivos.get(position).getArquivo().getName() + "</font></normal>"
                        + "<br><small><font color=\"#C7E4E6\">Tamanho: " + Util.calcularTamanho(tipo, arquivos.get(position).getArquivo().length()) + " " + tipo + "</font>");
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

        addEtArquivo = findViewById(R.id.addEtArquivo);
        addEtTamanho = findViewById(R.id.addEtTamanho);
        addSprTamanho = findViewById(R.id.addSprTamanho);
        Button addBtSelecionar = findViewById(R.id.addBtSelecionar);
        Button addBtAdicionar = findViewById(R.id.addBtAdicionar);
        addLvArquivos = findViewById(R.id.addLvArquivos);
        addBtRemover = findViewById(R.id.addBtRemover);
        Button addBtRemoverTodos = findViewById(R.id.addBtRemoverTodos);
        addEtTamanhoTotal = findViewById(R.id.addEtTamanhoTotal);
        addSprTamanhoTotal = findViewById(R.id.addSprTamanhoTotal);
        Button addBtConfirmar = findViewById(R.id.addBtConfirmar);
        Button addBtCancelar = findViewById(R.id.addBtCancelar);

        addEtArquivo.setInputType(InputType.TYPE_NULL);
        addEtTamanho.setInputType(InputType.TYPE_NULL);
        addEtTamanhoTotal.setInputType(InputType.TYPE_NULL);

        Intent intent = getIntent();
        arquivos = (ArrayList<Arquivo>) intent.getSerializableExtra("arquivos");
        tamanhoTotal = intent.getLongExtra("tamanhoTotal", 0);

        addSprTamanho.setAdapter(Util.getSpinnerAdapter(this));
        addSprTamanho.setSelection(0);
        addSprTamanho.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (arquivo != null) {
                    addEtTamanho.setText(Util.calcularTamanho(addSprTamanho.getSelectedItem().toString(), arquivo.length()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        addBtSelecionar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialFilePicker()
                    .withActivity(AddArquivosActivity.this)
                    .withRequestCode(REQUEST_CODE_SELECIONAR_ARQUIVO)
                    .withFilterDirectories(true)
                    .withHiddenFiles(true)
                    .start();
            }
        });

        addBtAdicionar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adicionarArquivo();
            }
        });

        addLvArquivos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (selecionadoParaRemover != position) {
                    selecionadoParaRemover = position;
                    addBtRemover.setText("Remover [" + selecionadoParaRemover + "]");
                } else {
                    selecionadoParaRemover = -1;
                    addBtRemover.setText("Remover");
                }
            }
        });

        addBtRemover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removerArquivo();
            }
        });

        addBtRemoverTodos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removerTodosArquivos();
            }
        });

        addSprTamanhoTotal.setAdapter(Util.getSpinnerAdapter(this));
        addSprTamanhoTotal.setSelection(0);
        addSprTamanhoTotal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (arquivos != null && !arquivos.isEmpty()) {
                    atualizarLvArquivos();
                    addEtTamanhoTotal.setText(Util.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        addBtConfirmar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (arquivos.size() > 1) {
                    Intent intent = new Intent();
                    intent.putExtra("arquivos", arquivos);
                    intent.putExtra("tamanhoTotal", tamanhoTotal);
                    setResult(RESULT_OK, intent);
                    finish();
                } else {
                    exibirMensagem("Erro: Adicione pelo menos 2 (dois) arquivos.");
                }
            }
        });

        addBtCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arquivos != null && !arquivos.isEmpty()) {
            atualizarLvArquivos();
            addEtTamanhoTotal.setText(Util.calcularTamanho(addSprTamanhoTotal.getSelectedItem().toString(), tamanhoTotal));
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECIONAR_ARQUIVO && resultCode == RESULT_OK) {
            arquivo = new File(data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH));
            addEtArquivo.setText(arquivo.getName());
            addEtTamanho.setText(Util.calcularTamanho(addSprTamanho.getSelectedItem().toString(), arquivo.length()));
        }
    }
}

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

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import br.com.iagocolodetti.transferirarquivo.exception.ClienteConectarException;
import br.com.iagocolodetti.transferirarquivo.exception.EnviarArquivoException;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_SELECIONAR_ARQUIVO = 1000;
    private final int REQUEST_CODE_INTERNET = 1001;
    private final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1002;
    private final int REQUEST_CODE_ADD_ARQUIVOS = 1003;

    private Cliente cliente = null;

    private ArrayList<Arquivo> arquivos = null;
    private long tamanhoTotal = 0;

    private EditText etIP, etPorta, etArmazenamento, etArquivo, etTamanho, etStatus;
    private TextView tvProgresso, tvAreaLog;
    private RadioButton rbInterno, rbExterno;
    private Button btConectar;
    private CheckBox cbLote;
    private Spinner sprTamanho;
    private ProgressBar pbStatus;
    private ScrollView svAreaLog;

    // <editor-fold defaultstate="collapsed" desc="Métodos">
    public void exibirMensagem(final String mensagem) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, mensagem, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void addAreaLog(final String mensagem) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvAreaLog.append((tvAreaLog.getText().toString().isEmpty() ? "" : "\n") + String.format("[%s] %s", new SimpleDateFormat("HH:mm:ss").format(new Date()), mensagem));
                svAreaLog.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        svAreaLog.fullScroll(View.FOCUS_DOWN);
                    }
                }, 100);
            }
        });
    }

    private void conectar() {
        hideSoftKeyboard(MainActivity.this);
        if (!etPorta.getText().toString().isEmpty()) {
            int porta;
            try {
                porta = Integer.parseInt(etPorta.getText().toString());
                cliente.conectar(etIP.getText().toString(), porta, getDiretorioSelecionado());
            } catch (NumberFormatException e) {
                exibirMensagem("Erro: A porta deve ser definida com número inteiro.");
            } catch (ClienteConectarException e) {
                exibirMensagem("Erro: " + e.getMessage());
            }
        } else {
            exibirMensagem("Erro: Defina o número da porta.");
        }
    }

    public void conectando() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btConectar.setText("CONECTANDO");
                etIP.setInputType(InputType.TYPE_NULL);
                etPorta.setInputType(InputType.TYPE_NULL);
                rbInterno.setEnabled(false);
                rbExterno.setEnabled(false);
            }
        });
    }

    public void conectado() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btConectar.setText("DESCONECTAR");
            }
        });
    }

    private void desconectar() {
        cliente.desconectar();
    }

    public void desconectado() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                etIP.setInputType(InputType.TYPE_CLASS_NUMBER);
                etIP.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
                etPorta.setInputType(InputType.TYPE_CLASS_NUMBER);
                rbInterno.setEnabled(true);
                rbExterno.setEnabled(true);
                btConectar.setText("CONECTAR");
            }
        });
    }

    public void setStatus(final String status) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                etStatus.setText(status);
            }
        });
    }

    public void setStatusProgress(final int valor) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbStatus.setProgress(valor);
                tvProgresso.setText((valor > 0 ? valor + "%" : ""));
            }
        });
    }

    public void permanecerAcordado(final boolean state) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
    }

    private InputFilter[] filtroIP() {
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart) +
                            source.subSequence(start, end) +
                            destTxt.substring(dend);
                    if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }
        };
        return filters;
    }
    // </editor-fold>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.INTERNET}, REQUEST_CODE_INTERNET);
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }
        }

        final Toolbar mytoolbar = findViewById(R.id.mytoolbar);
        setSupportActionBar(mytoolbar);
        mytoolbar.setSubtitle(Html.fromHtml("<small><i><font color=\"#AAAAAA\">" + getString(R.string.site) + "</font></i></small><br>"));
        mytoolbar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.site_url))));
            }
        });

        cliente = new Cliente(this);

        arquivos = new ArrayList<>();

        etIP = findViewById(R.id.etIP);
        etPorta = findViewById(R.id.etPorta);
        etArmazenamento = findViewById(R.id.etArmazenamento);
        rbInterno = findViewById(R.id.rbInterno);
        rbExterno = findViewById(R.id.rbExterno);
        btConectar = findViewById(R.id.btConectar);

        etArquivo = findViewById(R.id.etArquivo);
        etTamanho = findViewById(R.id.etTamanho);
        sprTamanho = findViewById(R.id.sprTamanho);
        Button btSelecionar = findViewById(R.id.btSelecionar);
        cbLote = findViewById(R.id.cbLote);
        Button btEnviar = findViewById(R.id.btEnviar);

        etStatus = findViewById(R.id.etStatus);
        pbStatus = findViewById(R.id.pbStatus);
        tvProgresso = findViewById(R.id.tvProgresso);

        tvAreaLog = findViewById(R.id.tvAreaLog);
        svAreaLog = findViewById(R.id.svAreaLog);

        etIP.setFilters(filtroIP());
        etArmazenamento.setInputType(InputType.TYPE_NULL);
        etArquivo.setInputType(InputType.TYPE_NULL);
        etTamanho.setInputType(InputType.TYPE_NULL);
        etStatus.setInputType(InputType.TYPE_NULL);

        rbInterno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etArmazenamento.setText(getDiretorioSelecionado());
            }
        });

        rbExterno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                etArmazenamento.setText(getDiretorioSelecionado());
            }
        });

        btConectar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (btConectar.getText().equals("CONECTAR")) {
                    conectar();
                } else if (btConectar.getText().equals("DESCONECTAR") || btConectar.getText().equals("CONECTANDO")) {
                    desconectar();
                }
            }
        });

        sprTamanho.setAdapter(Util.getSpinnerAdapter(this));
        sprTamanho.setSelection(0);
        sprTamanho.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!arquivos.isEmpty()) {
                    etTamanho.setText(Util.calcularTamanho(sprTamanho.getSelectedItem().toString(), tamanhoTotal));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btSelecionar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!cliente.enviandoArquivos()) {
                    if (!cbLote.isChecked()) {
                        new MaterialFilePicker()
                                .withActivity(MainActivity.this)
                                .withRequestCode(REQUEST_CODE_SELECIONAR_ARQUIVO)
                                .withFilterDirectories(true)
                                .withHiddenFiles(true)
                                .start();
                    } else {
                        Intent intent = new Intent(MainActivity.this, AddArquivosActivity.class);
                        intent.putExtra("arquivos", arquivos);
                        intent.putExtra("tamanhoTotal", tamanhoTotal);
                        startActivityForResult(intent, REQUEST_CODE_ADD_ARQUIVOS);
                    }
                } else {
                    exibirMensagem("Erro: Você ainda está enviando arquivos, aguarde o termino da operação atual.");
                }
            }
        });

        btEnviar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (btConectar.getText().equals("DESCONECTAR")) {
                        try {
                            cliente.enviarArquivos(arquivos);
                        } catch (EnviarArquivoException e) {
                            exibirMensagem("Erro: " + e.getMessage());
                        }
                    } else {
                        exibirMensagem("Erro: Conecte-se a um servidor.");
                    }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        restaurarPreferencias();
    }

    @Override
    protected void onStop() {
        super.onStop();
        salvarPreferencias();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECIONAR_ARQUIVO && resultCode == RESULT_OK) {
            arquivos.clear();
            arquivos.add(new Arquivo(new File(data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH))));
            etArquivo.setText(arquivos.get(0).getArquivo().getName());
            tamanhoTotal = arquivos.get(0).getArquivo().length();
            etTamanho.setText(Util.calcularTamanho(sprTamanho.getSelectedItem().toString(), tamanhoTotal));
        }
        if (requestCode == REQUEST_CODE_ADD_ARQUIVOS && resultCode == RESULT_OK) {
            arquivos = (ArrayList<Arquivo>) data.getSerializableExtra("arquivos");
            tamanhoTotal = data.getLongExtra("tamanhoTotal", 0);
            etArquivo.setText("Vários");
            etTamanho.setText(Util.calcularTamanho(sprTamanho.getSelectedItem().toString(), tamanhoTotal));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_INTERNET:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    exibirMensagem("Erro: É necessário conceder permissão de acesso a internet.");
                    finish();
                }
                break;
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    exibirMensagem("Erro: É necessário conceder permissão de acesso ao armazenamento.");
                    finish();
                }
                break;
        }
    }

    public static void hideSoftKeyboard(AppCompatActivity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void salvarPreferencias() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("etIP", etIP.getText().toString());
        editor.putString("etPorta", etPorta.getText().toString());
        editor.putBoolean("rbInterno", rbInterno.isChecked());
        editor.putBoolean("cbLote", cbLote.isChecked());
        editor.apply();
    }

    private void restaurarPreferencias() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        etIP.setText(preferences.getString("etIP", ""));
        etPorta.setText(preferences.getString("etPorta", ""));
        rbExterno.setChecked(preferences.getBoolean("rbExterno", true));
        etArmazenamento.setText(getDiretorioSelecionado());
        cbLote.setChecked(preferences.getBoolean("cbLote", false));
    }

    private String getDiretorioSelecionado() {
        if(rbExterno.isChecked() && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";
        } else {
            if (!rbInterno.isChecked()) {
                rbInterno.setChecked(true);
                exibirMensagem("Erro: Não foi possível encontrar o cartão de memória.");
            }
            return getFilesDir() + "/" + Environment.DIRECTORY_DOWNLOADS + "/";
        }
    }
}

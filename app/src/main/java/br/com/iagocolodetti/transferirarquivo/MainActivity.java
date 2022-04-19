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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import androidx.preference.PreferenceManager;

import android.provider.Settings;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import br.com.iagocolodetti.transferirarquivo.exception.ClienteConectarException;
import br.com.iagocolodetti.transferirarquivo.exception.EnviarArquivoException;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_INTERNET = 1001;
    private final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1002;
    private final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1003;

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
        this.runOnUiThread(() -> Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SimpleDateFormat")
    public void addAreaLog(final String mensagem) {
        this.runOnUiThread(() -> {
            tvAreaLog.append((tvAreaLog.getText().toString().isEmpty() ? "" : "\n") + String.format("[%s] %s", new SimpleDateFormat("HH:mm:ss").format(new Date()), mensagem));
            svAreaLog.postDelayed(() -> svAreaLog.fullScroll(View.FOCUS_DOWN), 100);
        });
    }

    private void conectar() {
        hideSoftKeyboard(this);
        if (!etPorta.getText().toString().isEmpty()) {
            int porta;
            try {
                porta = Integer.parseInt(etPorta.getText().toString());
                cliente.conectar(etIP.getText().toString(), porta, getDiretorioSelecionado());
            } catch (NumberFormatException ex) {
                exibirMensagem(getString(R.string.erro_porta_nao_inteiro));
            } catch (ClienteConectarException ex) {
                exibirMensagem(getString(R.string.erro_com_parametro, ex.getMessage()));
            }
        } else {
            exibirMensagem(getString(R.string.erro_porta_nao_definida));
        }
    }

    public void conectando() {
        this.runOnUiThread(() -> {
            btConectar.setText(getString(R.string.bt_conectar_conectando));
            etIP.setInputType(InputType.TYPE_NULL);
            etPorta.setInputType(InputType.TYPE_NULL);
            rbInterno.setEnabled(false);
            rbExterno.setEnabled(false);
        });
    }

    public void conectado() {
        this.runOnUiThread(() -> btConectar.setText(getString(R.string.bt_conectar_desconectar)));
    }

    private void desconectar() {
        cliente.desconectar();
    }

    public void desconectado() {
        this.runOnUiThread(() -> {
            etIP.setInputType(InputType.TYPE_CLASS_NUMBER);
            etIP.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
            etPorta.setInputType(InputType.TYPE_CLASS_NUMBER);
            rbInterno.setEnabled(true);
            rbExterno.setEnabled(true);
            btConectar.setText(getString(R.string.bt_conectar));
        });
    }

    public void limparArquivos() {
        this.runOnUiThread(() -> {
            etArquivo.setText("");
            etTamanho.setText("");
        });
        arquivos.clear();
        tamanhoTotal = 0;
    }

    public void setStatus(final String status) {
        this.runOnUiThread(() -> etStatus.setText(status));
    }

    public void setStatusProgress(final int valor) {
        this.runOnUiThread(() -> {
            pbStatus.setProgress(valor);
            tvProgresso.setText((valor > 0 ? valor + "%" : ""));
        });
    }

    public void permanecerAcordado(final boolean state) {
        this.runOnUiThread(() -> {
            if (state) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    private InputFilter[] filtroIP() {
        InputFilter[] filters = new InputFilter[1];
        filters[0] = (source, start, end, dest, dstart, dend) -> {
            if (end > start) {
                String destTxt = dest.toString();
                String resultingTxt = destTxt.substring(0, dstart) +
                        source.subSequence(start, end) +
                        destTxt.substring(dend);
                if (!resultingTxt.matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                    return "";
                } else {
                    String[] splits = resultingTxt.split("\\.");
                    for (String split : splits) {
                        if (Integer.parseInt(split) > 255) {
                            return "";
                        }
                    }
                }
            }
            return null;
        };
        return filters;
    }
    // </editor-fold>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.INTERNET }, REQUEST_CODE_INTERNET);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                storageAccessResult.launch(intent);
            } catch (Exception ex) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storageAccessResult.launch(intent);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, REQUEST_CODE_READ_EXTERNAL_STORAGE);
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }
        }

        final Toolbar myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setSubtitle(Html.fromHtml("<small><i><font color=\"#AAAAAA\">" + getString(R.string.site) + "</font></i></small><br>", Html.FROM_HTML_MODE_LEGACY));
        myToolbar.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.site_url)))));

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

        rbInterno.setOnClickListener(view -> etArmazenamento.setText(getDiretorioSelecionado()));

        rbExterno.setOnClickListener(view -> etArmazenamento.setText(getDiretorioSelecionado()));

        btConectar.setOnClickListener(view -> {
            if (btConectar.getText().equals(getString(R.string.bt_conectar))) {
                conectar();
            } else if (btConectar.getText().equals(getString(R.string.bt_conectar_desconectar)) || btConectar.getText().equals(getString(R.string.bt_conectar_conectando))) {
                desconectar();
            }
        });

        sprTamanho.setAdapter(Utils.getSpinnerAdapter(this));
        sprTamanho.setSelection(0);
        sprTamanho.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!arquivos.isEmpty()) {
                    etTamanho.setText(Utils.calcularTamanho(sprTamanho.getSelectedItem().toString(), tamanhoTotal));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btSelecionar.setOnClickListener(view -> {
            if (!cliente.enviandoArquivos()) {
                if (!cbLote.isChecked()) {
                    Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                    chooseFile.setType("*/*");
                    chooseFile.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    chooseFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    selecionarArquivo.launch(Intent.createChooser(chooseFile, getString(R.string.titulo_selecionar_arquivo)));
                } else {
                    Intent intent = new Intent(this, AddArquivosActivity.class);
                    ArquivoHelper.setArquivos(arquivos);
                    ArquivoHelper.setTamanhoTotal(tamanhoTotal);
                    addArquivo.launch(intent);
                }
            } else {
                exibirMensagem(getString(R.string.erro_ainda_enviando_arquivo));
            }
        });

        btEnviar.setOnClickListener(view -> {
            if (btConectar.getText().equals(getString(R.string.bt_conectar_desconectar))) {
                try {
                    cliente.enviarArquivos(arquivos);
                } catch (EnviarArquivoException ex) {
                    exibirMensagem(getString(R.string.erro_com_parametro, ex.getMessage()));
                }
            } else {
                exibirMensagem(getString(R.string.erro_nao_conectado));
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

    ActivityResultLauncher<Intent> storageAccessResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        exibirMensagem(getString(R.string.erro_permissao_armazenamento));
                        finish();
                    }
                }
            }
        }
    );

    ActivityResultLauncher<Intent> selecionarArquivo = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent != null) {
                    try {
                        Uri uri = intent.getData();
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        Arquivo arquivo = new Arquivo(this, uri);
                        arquivos.clear();
                        arquivos.add(arquivo);
                        etArquivo.setText(arquivos.get(0).getName());
                        tamanhoTotal = arquivos.get(0).getSize();
                        etTamanho.setText(Utils.calcularTamanho(sprTamanho.getSelectedItem().toString(), tamanhoTotal));
                    } catch (FileNotFoundException ex) {
                        exibirMensagem(getString(R.string.erro_selecionar_arquivo));
                    }
                }
            }
        }
    );

    ActivityResultLauncher<Intent> addArquivo = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                arquivos = new ArrayList<>(ArquivoHelper.getArquivos());
                tamanhoTotal = ArquivoHelper.getTamanhoTotal();
                etArquivo.setText(R.string.et_arquivo_varios);
                etTamanho.setText(Utils.calcularTamanho(sprTamanho.getSelectedItem().toString(), tamanhoTotal));
                ArquivoHelper.clear();
            }
        }
    );

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_INTERNET:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    exibirMensagem(getString(R.string.erro_permissao_internet));
                    finish();
                }
                break;
            case REQUEST_CODE_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    exibirMensagem(getString(R.string.erro_permissao_armazenamento));
                    finish();
                }
                break;
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    exibirMensagem(getString(R.string.erro_permissao_armazenamento));
                    finish();
                }
                break;
        }
    }

    public static void hideSoftKeyboard(AppCompatActivity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
            View focusedView = activity.getCurrentFocus();
            if (focusedView != null) {
                inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
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
                exibirMensagem(getString(R.string.erro_cartao_sd));
            }
            return getFilesDir() + "/" + Environment.DIRECTORY_DOWNLOADS + "/";
        }
    }
}

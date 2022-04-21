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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import br.com.iagocolodetti.transferirarquivo.exception.ClientConnectException;
import br.com.iagocolodetti.transferirarquivo.exception.SendFileException;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_INTERNET = 1001;
    private final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1002;
    private final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1003;

    private Client client = null;

    private ArrayList<MyFile> myFiles = null;
    private long totalSize = 0;

    private EditText etxIP, etxPort, etxStorage, etxFile, etxSize, etxStatus;
    private TextView txvProgress, txvAreaLog;
    private RadioButton rdoInternal, rdoExternal;
    private Button btnConnect;
    private CheckBox chkBatch;
    private Spinner sprSize;
    private ProgressBar prgStatus;
    private ScrollView scvAreaLog;

    // <editor-fold defaultstate="collapsed" desc="Métodos">
    public void showMessage(final String message) {
        this.runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @SuppressLint("SimpleDateFormat")
    public void addAreaLog(final String message) {
        this.runOnUiThread(() -> {
            txvAreaLog.append((txvAreaLog.getText().toString().isEmpty() ? "" : "\n") + String.format("[%s] %s", new SimpleDateFormat("HH:mm:ss").format(new Date()), message));
            scvAreaLog.postDelayed(() -> scvAreaLog.fullScroll(View.FOCUS_DOWN), 100);
        });
    }

    private void connect() {
        hideSoftKeyboard(this);
        if (!etxPort.getText().toString().isEmpty()) {
            int port;
            try {
                port = Integer.parseInt(etxPort.getText().toString());
                client.connect(etxIP.getText().toString(), port, getSelectedDir());
            } catch (NumberFormatException ex) {
                showMessage(getString(R.string.port_not_integer_error));
            } catch (ClientConnectException ex) {
                showMessage(getString(R.string.parameter_error, ex.getMessage()));
            }
        } else {
            showMessage(getString(R.string.port_not_defined_error));
        }
    }

    public void connecting() {
        this.runOnUiThread(() -> {
            btnConnect.setText(getString(R.string.btn_connect_connecting));
            etxIP.setInputType(InputType.TYPE_NULL);
            etxPort.setInputType(InputType.TYPE_NULL);
            rdoInternal.setEnabled(false);
            rdoExternal.setEnabled(false);
        });
    }

    public void connected() {
        this.runOnUiThread(() -> btnConnect.setText(getString(R.string.btn_connect_disconnect)));
    }

    private void disconnect() {
        client.disconnect();
    }

    public void disconnected() {
        this.runOnUiThread(() -> {
            etxIP.setInputType(InputType.TYPE_CLASS_NUMBER);
            etxIP.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
            etxPort.setInputType(InputType.TYPE_CLASS_NUMBER);
            rdoInternal.setEnabled(true);
            rdoExternal.setEnabled(true);
            btnConnect.setText(getString(R.string.btn_connect));
        });
    }

    public void setStatus(final String status) {
        this.runOnUiThread(() -> etxStatus.setText(status));
    }

    public void setStatusProgress(final int valor) {
        this.runOnUiThread(() -> {
            prgStatus.setProgress(valor);
            txvProgress.setText((valor > 0 ? valor + "%" : ""));
        });
    }

    public void keepScreenOn(final boolean state) {
        this.runOnUiThread(() -> {
            if (state) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
    }

    private InputFilter[] ipFilter() {
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

        client = new Client(this);

        myFiles = new ArrayList<>();

        etxIP = findViewById(R.id.etxIP);
        etxPort = findViewById(R.id.etxPort);
        etxStorage = findViewById(R.id.etxStorage);
        rdoInternal = findViewById(R.id.rdoInternal);
        rdoExternal = findViewById(R.id.rdoExternal);
        btnConnect = findViewById(R.id.btnConnect);

        etxFile = findViewById(R.id.etxFile);
        etxSize = findViewById(R.id.etxSize);
        sprSize = findViewById(R.id.sprSize);
        Button btnSelect = findViewById(R.id.btnSelect);
        chkBatch = findViewById(R.id.chkBatch);
        Button btnSend = findViewById(R.id.btnSend);

        etxStatus = findViewById(R.id.etxStatus);
        prgStatus = findViewById(R.id.prgStatus);
        txvProgress = findViewById(R.id.txvProgress);

        txvAreaLog = findViewById(R.id.txvAreaLog);
        scvAreaLog = findViewById(R.id.scvAreaLog);

        etxIP.setFilters(ipFilter());
        etxStorage.setInputType(InputType.TYPE_NULL);
        etxFile.setInputType(InputType.TYPE_NULL);
        etxSize.setInputType(InputType.TYPE_NULL);
        etxStatus.setInputType(InputType.TYPE_NULL);

        rdoInternal.setOnClickListener(view -> etxStorage.setText(getSelectedDir()));

        rdoExternal.setOnClickListener(view -> etxStorage.setText(getSelectedDir()));

        btnConnect.setOnClickListener(view -> {
            if (btnConnect.getText().equals(getString(R.string.btn_connect))) {
                connect();
            } else if (btnConnect.getText().equals(getString(R.string.btn_connect_disconnect)) || btnConnect.getText().equals(getString(R.string.btn_connect_connecting))) {
                disconnect();
            }
        });

        sprSize.setAdapter(Utils.getSpinnerAdapter(this));
        sprSize.setSelection(0);
        sprSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!myFiles.isEmpty()) {
                    etxSize.setText(Utils.bytesTo(totalSize, sprSize.getSelectedItem().toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        btnSelect.setOnClickListener(view -> {
            if (!client.sendingFiles()) {
                if (!chkBatch.isChecked()) {
                    Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
                    chooseFile.setType("*/*");
                    chooseFile.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    chooseFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    chooseFileResult.launch(Intent.createChooser(chooseFile, getString(R.string.select_file_title)));
                } else {
                    Intent intent = new Intent(this, AddFilesActivity.class);
                    MyFileHelper.setMyFiles(myFiles);
                    MyFileHelper.setTotalSize(totalSize);
                    addFilesResult.launch(intent);
                }
            } else {
                showMessage(getString(R.string.still_sending_file_error));
            }
        });

        btnSend.setOnClickListener(view -> {
            if (btnConnect.getText().equals(getString(R.string.btn_connect_disconnect))) {
                try {
                    client.sendFiles(myFiles);
                } catch (SendFileException ex) {
                    showMessage(getString(R.string.parameter_error, ex.getMessage()));
                }
            } else {
                showMessage(getString(R.string.not_connected_error));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        restorePreferences();
    }

    @Override
    protected void onStop() {
        super.onStop();
        savePreferences();
    }

    ActivityResultLauncher<Intent> storageAccessResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        showMessage(getString(R.string.storage_permission_error));
                        finish();
                    }
                }
            }
        }
    );

    ActivityResultLauncher<Intent> chooseFileResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent != null) {
                    try {
                        Uri uri = intent.getData();
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        MyFile myFile = new MyFile(this, uri);
                        myFiles.clear();
                        myFiles.add(myFile);
                        etxFile.setText(myFiles.get(0).getName());
                        totalSize = myFiles.get(0).getSize();
                        etxSize.setText(Utils.bytesTo(totalSize, sprSize.getSelectedItem().toString()));
                    } catch (FileNotFoundException ex) {
                        showMessage(getString(R.string.select_file_error));
                    }
                }
            }
        }
    );

    ActivityResultLauncher<Intent> addFilesResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                myFiles = new ArrayList<>(MyFileHelper.getMyFiles());
                totalSize = MyFileHelper.getTotalSize();
                etxFile.setText(R.string.etx_file_various);
                etxSize.setText(Utils.bytesTo(totalSize, sprSize.getSelectedItem().toString()));
                MyFileHelper.clear();
            }
        }
    );

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_INTERNET:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showMessage(getString(R.string.internet_permission_error));
                    finish();
                }
                break;
            case REQUEST_CODE_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showMessage(getString(R.string.storage_permission_error));
                    finish();
                }
                break;
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showMessage(getString(R.string.storage_permission_error));
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

    private void savePreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("etIP", etxIP.getText().toString());
        editor.putString("etPorta", etxPort.getText().toString());
        editor.putBoolean("rbInterno", rdoInternal.isChecked());
        editor.putBoolean("cbLote", chkBatch.isChecked());
        editor.apply();
    }

    private void restorePreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        etxIP.setText(preferences.getString("etIP", ""));
        etxPort.setText(preferences.getString("etPorta", ""));
        rdoExternal.setChecked(preferences.getBoolean("rbExterno", true));
        etxStorage.setText(getSelectedDir());
        chkBatch.setChecked(preferences.getBoolean("cbLote", false));
    }

    private String getSelectedDir() {
        if(rdoExternal.isChecked() && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/";
        } else {
            if (!rdoInternal.isChecked()) {
                rdoInternal.setChecked(true);
                showMessage(getString(R.string.sdcard_error));
            }
            return getFilesDir() + "/" + Environment.DIRECTORY_DOWNLOADS + "/";
        }
    }
}

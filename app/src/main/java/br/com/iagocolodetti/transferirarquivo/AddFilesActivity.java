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
public class AddFilesActivity extends AppCompatActivity {

    private ArrayList<MyFile> myFiles = null;
    private long totalSize = 0;
    private int selectToRemove = -1;

    private EditText addEtxTotalSize;
    private Spinner addSprTotalSize;
    private ListView addLvwFiles;
    private Button addBtnRemove;

    // <editor-fold defaultstate="collapsed" desc="Métodos">
    private void showMessage(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void removeFile() {
        if (selectToRemove != -1) {
            totalSize -= myFiles.get(selectToRemove).getSize();
            myFiles.remove(selectToRemove);
            addBtnRemove.setText(getString(R.string.add_btn_remove));
            updateLvwFiles();
            addEtxTotalSize.setText(Utils.bytesTo(totalSize, addSprTotalSize.getSelectedItem().toString()));
            selectToRemove = -1;
        }
    }

    private void removeAllFiles() {
        myFiles.clear();
        updateLvwFiles();
        totalSize = 0;
        addEtxTotalSize.setText("");
    }

    private void updateLvwFiles() {
        final String bytesTo = addSprTotalSize.getSelectedItem().toString();
        ArrayAdapter<MyFile> adapter = new ArrayAdapter<MyFile>(this, R.layout.custom_listview, myFiles) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                Spanned spanned = Html.fromHtml("<normal><font color=\"#FF0000\">[" + position + "]</font><font color=\"#FBFBFB\"> " + myFiles.get(position).getName() + "</font></normal>"
                        + "<br><small><font color=\"#C7E4E6\">Tamanho: " + Utils.bytesTo(myFiles.get(position).getSize(), bytesTo) + " " + bytesTo + "</font>", Html.FROM_HTML_MODE_LEGACY);
                text1.setText(spanned);
                return view;
            }
        };
        addLvwFiles.setAdapter(adapter);
    }
    // </editor-fold>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_files);

        Button addBtnSelect = findViewById(R.id.addBtnSelect);
        addLvwFiles = findViewById(R.id.addLvwFiles);
        Button addBtnRemoveAll = findViewById(R.id.addBtnRemoveAll);
        addBtnRemove = findViewById(R.id.addBtnRemove);
        addEtxTotalSize = findViewById(R.id.addEtxTotalSize);
        addSprTotalSize = findViewById(R.id.addSprTotalSize);
        Button addBtnCancel = findViewById(R.id.addBtnCancel);
        Button addBtnConfirm = findViewById(R.id.addBtnConfirm);

        addEtxTotalSize.setInputType(InputType.TYPE_NULL);

        myFiles = new ArrayList<>(MyFileHelper.getMyFiles());
        totalSize = MyFileHelper.getTotalSize();
        MyFileHelper.clear();

        addBtnSelect.setOnClickListener(view -> {
            Intent chooseFiles = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            chooseFiles.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFiles.setType("*/*");
            chooseFiles.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            chooseFiles.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            chooseFiles.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            chooseFilesResult.launch(Intent.createChooser(chooseFiles, getString(R.string.select_files_title)));
        });

        addLvwFiles.setOnItemClickListener((parent, view, position, id) -> {
            if (selectToRemove != position) {
                selectToRemove = position;
                addBtnRemove.setText(getString(R.string.add_btn_remove_id, selectToRemove));
            } else {
                selectToRemove = -1;
                addBtnRemove.setText(getString(R.string.add_btn_remove));
            }
        });

        addBtnRemoveAll.setOnClickListener(view -> removeAllFiles());

        addBtnRemove.setOnClickListener(view -> removeFile());

        addSprTotalSize.setAdapter(Utils.getSpinnerAdapter(this));
        addSprTotalSize.setSelection(0);
        addSprTotalSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (myFiles != null && !myFiles.isEmpty()) {
                    updateLvwFiles();
                    addEtxTotalSize.setText(Utils.bytesTo(totalSize, addSprTotalSize.getSelectedItem().toString()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        addBtnCancel.setOnClickListener(view -> finish());

        addBtnConfirm.setOnClickListener(view -> {
            if (myFiles.size() > 1) {
                MyFileHelper.setMyFiles(myFiles);
                MyFileHelper.setTotalSize(totalSize);
                setResult(RESULT_OK);
                finish();
            } else {
                showMessage(getString(R.string.confirm_files_error));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (myFiles != null && !myFiles.isEmpty()) {
            updateLvwFiles();
            addEtxTotalSize.setText(Utils.bytesTo(totalSize, addSprTotalSize.getSelectedItem().toString()));
        }
    }

    ActivityResultLauncher<Intent> chooseFilesResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                if (intent != null) {
                    try {
                        Uri uri;
                        int errors = 0;
                        if (intent.getClipData() != null) {
                            for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                                uri = intent.getClipData().getItemAt(i).getUri();
                                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                MyFile myFile = new MyFile(this, uri);
                                if (!myFiles.contains(myFile)) {
                                    myFiles.add(myFile);
                                    totalSize += myFile.getSize();
                                } else {
                                    errors++;
                                }
                            }
                            updateLvwFiles();
                            addEtxTotalSize.setText(Utils.bytesTo(totalSize, addSprTotalSize.getSelectedItem().toString()));
                            if (errors > 0) {
                                showMessage(getResources().getQuantityString(R.plurals.select_files_already_added_errors, errors));
                            }
                        } else {
                            uri = intent.getData();
                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            MyFile myFile = new MyFile(this, uri);
                            if (!myFiles.contains(myFile)) {
                                myFiles.add(myFile);
                                updateLvwFiles();
                                totalSize += myFile.getSize();
                                addEtxTotalSize.setText(Utils.bytesTo(totalSize, addSprTotalSize.getSelectedItem().toString()));
                            } else {
                                showMessage(getString(R.string.select_files_already_added_error));
                            }
                        }
                    } catch (FileNotFoundException ex) {
                        showMessage(getString(R.string.select_files_error));
                    }
                }
            }
        }
    );
}

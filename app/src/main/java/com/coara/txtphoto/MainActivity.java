package com.coara.txtphoto;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SELECT_FILE = 1;
    private static final long MAX_FILE_SIZE_BYTES = 40 * 1024;

    private TextView filePathView;
    private String selectedFilePath;
    private Button convertButton, revertButton;
    private Button resize25Button, resize50Button;
    private float scaleFactor = 1.0f;
    private boolean is25Selected = false;
    private boolean is50Selected = false;
    private boolean isConverting = false;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.txtphoto_main);

        Button selectFileButton = findViewById(R.id.selectFileButton);
        convertButton = findViewById(R.id.convertButton);
        revertButton = findViewById(R.id.revertButton);
        filePathView = findViewById(R.id.filePathView);
        resize25Button = findViewById(R.id.resize25Button);
        resize50Button = findViewById(R.id.resize50Button);

        selectFileButton.setOnClickListener(v -> selectFile());

        resize25Button.setOnClickListener(v -> {
            if (is25Selected) {
                is25Selected = false;
                scaleFactor = 1.0f;
                resize25Button.getBackground().setColorFilter(null);
                Toast.makeText(this, "25% 縮小を解除しました", Toast.LENGTH_SHORT).show();
            } else {
                is25Selected = true;
                is50Selected = false;
                scaleFactor = 0.25f;
                
                resize25Button.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0x00333333));
                resize50Button.getBackground().setColorFilter(null); 
                Toast.makeText(this, "25% に縮小が選択されました", Toast.LENGTH_SHORT).show();
            }
        });

        resize50Button.setOnClickListener(v -> {
            if (is50Selected) {
                is50Selected = false;
                scaleFactor = 1.0f;
                resize50Button.getBackground().setColorFilter(null);
                Toast.makeText(this, "50% 縮小を解除しました", Toast.LENGTH_SHORT).show();
            } else {
                is50Selected = true;
                is25Selected = false;
                scaleFactor = 0.5f;
            
                resize50Button.getBackground().setColorFilter(new LightingColorFilter(0xFFFFFFFF, 0x00333333));
                resize25Button.getBackground().setColorFilter(null);
                Toast.makeText(this, "50% に縮小が選択されました", Toast.LENGTH_SHORT).show();
            }
        });

        convertButton.setOnClickListener(v -> {
            if (selectedFilePath != null) {
                convertAsciiToImage(Uri.parse(selectedFilePath));
            }
        });

        revertButton.setOnClickListener(v -> {
            if (selectedFilePath != null) {
                convertDatToImage(Uri.parse(selectedFilePath));
            }
        });
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"text/plain", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                String fileName = getFileName(uri);
                if (fileName != null) {
                    selectedFilePath = uri.toString();
                    filePathView.setText(fileName);

                    String extension = "";
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex >= 0) {
                        extension = fileName.substring(dotIndex).toLowerCase();
                    }
                    if (".txt".equals(extension)) {
                        convertButton.setEnabled(true);
                        revertButton.setEnabled(false);
                    } else if (".dat".equals(extension)) {
                        convertButton.setEnabled(false);
                        revertButton.setEnabled(true);
                    } else {
                        Toast.makeText(this, "サポートされていないファイル形式です", Toast.LENGTH_SHORT).show();
                        convertButton.setEnabled(false);
                        revertButton.setEnabled(false);
                    }
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private long getFileSize(Uri uri) {
        long size = -1;
        if ("content".equals(uri.getScheme())) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index != -1) {
                        size = cursor.getLong(index);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return size;
    }

    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        builder.setView(progressBar);
        builder.setMessage("しばらくお待ち下さい");
        builder.setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void convertAsciiToImage(Uri uri) {
        long fileSize = getFileSize(uri);
        if (fileSize > MAX_FILE_SIZE_BYTES || fileSize == -1) {
            Toast.makeText(this, "ファイルが大きすぎます。40KB以内のファイルを選択してください", Toast.LENGTH_SHORT).show();
            return;
        }

        runOnUiThread(this::showProgressDialog);

        isConverting = true;

        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                StringBuilder asciiArt = new StringBuilder();
                String line;
                int maxWidth = 0;
                int lineCount = 0;

                Paint paint = new Paint();
                paint.setTextSize(40);
                paint.setTypeface(Typeface.MONOSPACE);
                paint.setAntiAlias(false);

                while ((line = reader.readLine()) != null) {
                    asciiArt.append(line).append("\n");
                    maxWidth = Math.max(maxWidth, (int) paint.measureText(line));
                    lineCount++;
                }

                if (lineCount == 0) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "空のファイルです", Toast.LENGTH_SHORT).show());
                    return;
                }

                int charHeight = (int) (paint.getTextSize() + 10);
                int width = maxWidth + 20;
                int height = lineCount * charHeight + 20;

                final Bitmap bitmap;
                try {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "メモリ不足です。縮小してください", Toast.LENGTH_SHORT).show());
                    return;
                }

                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);

                String[] lines = asciiArt.toString().split("\n");
                int y = charHeight;
                for (String l : lines) {
                    canvas.drawText(l, 10, y, paint);
                    y += charHeight;
                }

                final Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> processAndSaveBitmap(finalBitmap));

            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "変換中にエラーが発生しました", Toast.LENGTH_SHORT).show());
            } catch (OutOfMemoryError e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "メモリ不足です。ファイルを小さくするか縮小してください", Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> {
                    dismissProgressDialog();
                    isConverting = false;
                });
            }
        }).start();
    }

    private void convertDatToImage(Uri uri) {
        long fileSize = getFileSize(uri);
        if (fileSize > MAX_FILE_SIZE_BYTES || fileSize == -1) {
            Toast.makeText(this, "ファイルが大きすぎます。40KB以内のファイルを選択してください", Toast.LENGTH_SHORT).show();
            return;
        }

        runOnUiThread(this::showProgressDialog);

        isConverting = true;

        new Thread(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                List<int[]> pixelData = new ArrayList<>();
                int width = 0, height = 0;

                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length != 2) continue;
                    String[] position = parts[0].split(",");
                    String[] rgb = parts[1].split(",");
                    if (position.length != 2 || rgb.length != 3) continue;
                    int x = Integer.parseInt(position[0]);
                    int y = Integer.parseInt(position[1]);
                    int r = Integer.parseInt(rgb[0]);
                    int g = Integer.parseInt(rgb[1]);
                    int b = Integer.parseInt(rgb[2]);
                    pixelData.add(new int[]{x, y, r, g, b});
                    width = Math.max(width, x);
                    height = Math.max(height, y);
                }

                if (pixelData.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "空のファイルです", Toast.LENGTH_SHORT).show());
                    return;
                }

                width += 1;
                height += 1;

                Bitmap bitmap;
                try {
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                } catch (OutOfMemoryError e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "メモリ不足です。縮小してください", Toast.LENGTH_SHORT).show());
                    return;
                }

                for (int[] pixel : pixelData) {
                    int x = pixel[0];
                    int y = pixel[1];
                    int r = pixel[2];
                    int g = pixel[3];
                    int b = pixel[4];
                    bitmap.setPixel(x, y, Color.rgb(r, g, b));
                }

                final Bitmap finalBitmap = bitmap;
                runOnUiThread(() -> processAndSaveBitmap(finalBitmap));

            } catch (IOException | NumberFormatException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "変換中にエラーが発生しました", Toast.LENGTH_SHORT).show());
            } catch (OutOfMemoryError e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "メモリ不足です。ファイルを小さくするか縮小してください", Toast.LENGTH_SHORT).show());
            } finally {
                runOnUiThread(() -> {
                    dismissProgressDialog();
                    isConverting = false;
                });
            }
        }).start();
    }

    private void processAndSaveBitmap(Bitmap bitmap) {
        if (scaleFactor != 1.0f) {
            int newWidth = (int) (bitmap.getWidth() * scaleFactor);
            int newHeight = (int) (bitmap.getHeight() * scaleFactor);
            if (newWidth > 0 && newHeight > 0) {
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                bitmap.recycle();
                bitmap = scaledBitmap;
            }
            scaleFactor = 1.0f;
            is25Selected = false;
            is50Selected = false;
            runOnUiThread(() -> {
                resize25Button.getBackground().setColorFilter(null);
                resize50Button.getBackground().setColorFilter(null);
            });
        }
        saveBitmapAsPng(bitmap);
        bitmap.recycle();
    }

    private void saveBitmapAsPng(Bitmap bitmap) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, generateFileName() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/TxtPhoto");

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                runOnUiThread(() -> Toast.makeText(this, "画像が保存されました", Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "保存中にエラーが発生しました", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private String generateFileName() {
        return new SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    @Override
    public void onBackPressed() {
        if (isConverting) {
            return;
        }
        super.onBackPressed();
    }
}

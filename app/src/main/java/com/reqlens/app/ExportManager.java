package com.reqlens.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ExportManager {
    private ExportManager() {}

    public static File writeJson(Context context, List<FlowRecord> records, boolean redact) throws Exception {
        return writeText(context, "reqlens-flows-" + System.currentTimeMillis() + ".json",
                FlowExporter.toJson(records, redact));
    }

    public static File writeCsv(Context context, List<FlowRecord> records) throws Exception {
        return writeText(context, "reqlens-flows-" + System.currentTimeMillis() + ".csv",
                FlowExporter.toCsv(records));
    }

    public static File writeText(Context context, String name, String content) throws Exception {
        File dir = new File(context.getCacheDir(), "exports");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Could not create export directory");
        File file = new File(dir, name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    public static Intent share(Context context, File file, String mime) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".files", file);
        return new Intent(Intent.ACTION_SEND)
                .setType(mime)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
}

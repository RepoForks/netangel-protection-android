package com.netangel.netangelprotection.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.VpnService;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Base64;

import com.netangel.netangelprotection.NetAngelApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.fragments.Utils;

public class VpnHelper {
    private static final String TAG = VpnHelper.class.getSimpleName();

    private Context context;

    public VpnHelper(Context context) {
        this.context = context;
    }

    @WorkerThread
    public boolean importProfileFromFile(@NonNull Uri ovpnFileUri) {
        InputStreamReader isr = null;
        try {
            InputStream is = context.getContentResolver().openInputStream(ovpnFileUri);
            if (is == null) {
                return false;
            }

            isr = new InputStreamReader(is);
            ConfigParser cp = new ConfigParser();
            cp.parseConfig(isr);
            VpnProfile profile = cp.convertProfile();
            if (profile == null) {
                return false;
            }

            if (TextUtils.isEmpty(profile.mName)) {
                String name = ovpnFileUri.getLastPathSegment();
                if (name.lastIndexOf('/') != -1) {
                    name = name.substring(name.lastIndexOf('/') + 1);
                }
                profile.mName = name.replace(".ovpn", "");
            }

            List<String> pathSegments = ovpnFileUri.getPathSegments();
            profile.mCaFilename = embedFile(profile.mCaFilename, Utils.FileType.CA_CERTIFICATE,
                    pathSegments, false);
            profile.mClientCertFilename = embedFile(profile.mClientCertFilename,
                    Utils.FileType.CLIENT_CERTIFICATE, pathSegments, false);
            profile.mClientKeyFilename = embedFile(profile.mClientKeyFilename, Utils.FileType.KEYFILE,
                    pathSegments, false);
            profile.mTLSAuthFilename = embedFile(profile.mTLSAuthFilename, Utils.FileType.TLS_AUTH_FILE,
                    pathSegments, false);
            profile.mPKCS12Filename = embedFile(profile.mPKCS12Filename, Utils.FileType.PKCS12,
                    pathSegments, false);
            profile.mCrlFilename = embedFile(profile.mCrlFilename, Utils.FileType.CRL_FILE,
                    pathSegments, true);
            String embeddedPwFile = embedFile(cp.getAuthUserPassFile(), Utils.FileType.USERPW_FILE,
                    pathSegments, false);
            if (!TextUtils.isEmpty(embeddedPwFile)) {
                ConfigParser.useEmbbedUserAuth(profile, embeddedPwFile);
            }

            ProfileManager pm = ProfileManager.getInstance(context);
            pm.addProfile(profile);
            pm.saveProfile(context, profile);
            pm.saveProfileList(context);
            return true;
        } catch (Exception e) {
            LogUtils.w(TAG, "Failed to import profile from file, uri = " + ovpnFileUri, e);
            return false;
        } finally {
            CommonUtils.closeSilently(isr);
        }
    }

    private String embedFile(String fileName, Utils.FileType type, @NonNull List<String> pathSegments,
                                    boolean onlyFindFileAndNullOnNotFound) {
        if (fileName == null) {
            return null;
        }

        // Already embedded, nothing to do.
        if (VpnProfile.isEmbedded(fileName)) {
            return fileName;
        }

        File possibleFile = findFileRaw(fileName, pathSegments);
        if (possibleFile == null) {
            return onlyFindFileAndNullOnNotFound ? null : fileName;
        }
        if (onlyFindFileAndNullOnNotFound) {
            return possibleFile.getAbsolutePath();
        }
        return readFileContent(possibleFile, type == Utils.FileType.PKCS12);
    }

    private File findFileRaw(String fileName, @NonNull List<String> pathSegments) {
        if (TextUtils.isEmpty(fileName)) {
            return null;
        }

        // Try different path relative to /mnt/sdcard.
        File sdcard = Environment.getExternalStorageDirectory();
        File root = new File("/");
        HashSet<File> dirList = new HashSet<>();
        for (int i = pathSegments.size() - 1; i >= 0; i--) {
            String path = "";
            for (int j = 0; j <= i; j++) {
                path += "/" + pathSegments.get(j);
            }
            // Do a little hackish dance for the Android File Importer
            // /document/primary:ovpn/openvpn-imt.conf
            if (path.indexOf(':') != -1 && path.lastIndexOf('/') > path.indexOf(':')) {
                String possibleDir = path.substring(path.indexOf(':') + 1, path.length());
                possibleDir = possibleDir.substring(0, possibleDir.lastIndexOf('/'));
                dirList.add(new File(sdcard, possibleDir));
            }
            dirList.add(new File(path));
        }
        dirList.add(sdcard);
        dirList.add(root);

        String[] fileParts = fileName.split("/");
        for (File dir : dirList) {
            String suffix = "";
            for (int i = fileParts.length - 1; i >= 0; i--) {
                suffix = i == fileParts.length - 1 ? fileParts[i] : fileParts[i] + "/" + suffix;
                File possibleFile = new File(dir, suffix);
                if (possibleFile.canRead()) {
                    return possibleFile;
                }
            }
        }
        return null;
    }

    private String readFileContent(File possibleFile, boolean base64encode) {
        byte[] content;
        try {
            content = readBytesFromFile(possibleFile);
        } catch (IOException e) {
            return null;
        }

        String data = base64encode ? Base64.encodeToString(content, Base64.DEFAULT) : new String(content);
        return VpnProfile.DISPLAYNAME_TAG + possibleFile.getName() + VpnProfile.INLINE_TAG + data;
    }

    private byte[] readBytesFromFile(File file) throws IOException {
        long len = file.length();
        if (len > VpnProfile.MAX_EMBED_FILE_SIZE) {
            throw new IOException("File size of file to import too large.");
        }

        byte[] bytes = new byte[(int) len];
        InputStream input = new FileInputStream(file);
        int offset = 0;
        int bytesRead;
        while (offset < bytes.length
                && (bytesRead = input.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += bytesRead;
        }
        input.close();
        return bytes;
    }

    @Nullable
    public VpnProfile getProfile() {
        Collection<VpnProfile> profiles = ProfileManager.getInstance(context)
                .getProfiles();
        return profiles.isEmpty() ? null : profiles.iterator().next();
    }

    public void removeProfiles() {
        ProfileManager pm = ProfileManager.getInstance(context);
        while (!pm.getProfiles().isEmpty()) {
            VpnProfile profile = pm.getProfiles().iterator().next();
            pm.removeProfile(context, profile);
        }
    }

    @Nullable
    public Intent prepareVpnService() {
        Intent intent = VpnService.prepare(context);
        // Check if we want to fix /dev/tun
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean usecm9fix = prefs.getBoolean("useCM9Fix", false);
        boolean loadTunModule = prefs.getBoolean("loadTunModule", false);
        boolean isCmFixed = false;
        if (loadTunModule) {
            isCmFixed = executeSuCmd("insmod /system/lib/modules/tun.ko");
        }
        if (usecm9fix && !isCmFixed) {
            executeSuCmd("chown system /dev/tun");
        }
        return intent;
    }

    private boolean executeSuCmd(String command) {
        boolean isCmFixed = false;
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", command);
            Process p = pb.start();
            int ret = p.waitFor();
            if (ret == 0) {
                isCmFixed = true;
            }
        } catch (InterruptedException | IOException e) {
            VpnStatus.logException("SU command", e);
        }
        return isCmFixed;
    }
}

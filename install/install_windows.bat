if not exist "platform-tools/adb" (
    echo "Downloading Android Platform Tools"
    powershell.exe -Command "(New-Object Net.WebClient).DownloadFile('https://dl.google.com/android/repository/platform-tools-latest-windows.zip', 'platform-tools-latest-windows.zip')"
    powershell.exe -nologo -noprofile -command "& { $shell = New-Object -COM Shell.Application; $target = $shell.NameSpace('%cd%'); $zip = $shell.NameSpace('%cd%\platform-tools-latest-windows.zip'); $target.CopyHere($zip.Items(), 16); }"
)

if not exist "detoxdroid-latest.apk" (
    echo "Download latest DetoxDroid APK"
    powershell.exe -Command "https://github.com/flxapps/DetoxDroid/releases/latest/download/app-release.apk', 'detoxdroid-latest.apk')"
)

echo "Installing Detox Droid on your device"
%cd%\platform-tools\adb.exe install -r detoxdroid-latest.apk

echo "Granting Permissions"
%cd%\platform-tools\adb.exe shell pm grant com.flx_apps.digitaldetox android.permission.WRITE_SECURE_SETTINGS
%cd%\platform-tools\adb.exe shell pm grant com.flx_apps.digitaldetox android.permission.ACCESS_NOTIFICATION_POLICY

echo "Starting App"
%cd%\platform-tools\adb.exe shell monkey -p com.flx_apps.digitaldetox 1

echo "Done."


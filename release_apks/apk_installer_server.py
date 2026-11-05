import http.server
import os
import socketserver

PORT = 8899
RELEASE_DIR = r"F:\word và Excel\release_apks"
DOCUMENTS_APK = os.path.join(RELEASE_DIR, "EW_Documents_v0.2.0_native_light.apk")
SHEETS_APK = os.path.join(RELEASE_DIR, "EW_Sheets_v0.2.0_native_light.apk")

class EWPackageInstallerHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=RELEASE_DIR, **kwargs)

    def do_GET(self):
        clean_path = self.path.split("?")[0]
        if clean_path in ("/install-documents.apk", "/EW-Documents.apk", "/documents.apk", "/EW_Documents_v0.2.0_native_light.apk"):
            self.serve_apk_file(DOCUMENTS_APK, "EW-Documents-Native-Light.apk", head_only=False)
            return
        elif clean_path in ("/install-sheets.apk", "/EW-Sheets.apk", "/sheets.apk", "/EW_Sheets_v0.2.0_native_light.apk"):
            self.serve_apk_file(SHEETS_APK, "EW-Sheets-Native-Light.apk", head_only=False)
            return
        super().do_GET()

    def do_HEAD(self):
        clean_path = self.path.split("?")[0]
        if clean_path in ("/install-documents.apk", "/EW-Documents.apk", "/documents.apk", "/EW_Documents_v0.2.0_native_light.apk"):
            self.serve_apk_file(DOCUMENTS_APK, "EW-Documents-Native-Light.apk", head_only=True)
            return
        elif clean_path in ("/install-sheets.apk", "/EW-Sheets.apk", "/sheets.apk", "/EW_Sheets_v0.2.0_native_light.apk"):
            self.serve_apk_file(SHEETS_APK, "EW-Sheets-Native-Light.apk", head_only=True)
            return
        super().do_HEAD()

    def serve_apk_file(self, file_path, download_name, head_only=False):
        if not os.path.exists(file_path):
            self.send_error(404, f"APK file not found: {download_name}")
            return
        file_size = os.path.getsize(file_path)
        self.send_response(200)
        # Official Android Package Installer MIME type + nosniff prevents phone from treating APK as ZIP
        self.send_header("Content-Type", "application/vnd.android.package-archive")
        self.send_header("Content-Disposition", f'attachment; filename="{download_name}"')
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Content-Length", str(file_size))
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate")
        self.end_headers()
        if not head_only:
            with open(file_path, "rb") as f:
                while chunk := f.read(1024 * 1024):  # 1MB buffer
                    self.wfile.write(chunk)

if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.ThreadingTCPServer(("0.0.0.0", PORT), EWPackageInstallerHandler) as httpd:
        print(f"EW Office Installer Server running on 0.0.0.0:{PORT}")
        httpd.serve_forever()

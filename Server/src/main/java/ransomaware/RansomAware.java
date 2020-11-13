package ransomaware;

import ransomaware.exceptions.UnauthorizedException;

import java.io.File;

public class RansomAware {

    public RansomAware(String path, int port, boolean firstTime) {
        if (!firstTime) {
            // validateFS()
        }

        Server.start(this, port);
    }

    private boolean hasAccessToFile(String user, String fileName) {
        return user.equals(fileName.split("/")[0]);
    }

    private void firstTimeSetup() {
        File file = new File(ServerVariables.FS_PATH);
        boolean success = file.mkdir();
        if (success) {
            System.out.println("Created fs folder successfully");
        }
    }

    public void uploadFile(int sessionToken, String file, byte[] data) {
        String user = SessionManager.getUsername(sessionToken);
        String fileName = FileManager.getFileName(user, file);
        if (hasAccessToFile(user, fileName)) {
            FileManager.saveFile(fileName, data);
        } else {
            throw new UnauthorizedException();
        }
    }
}

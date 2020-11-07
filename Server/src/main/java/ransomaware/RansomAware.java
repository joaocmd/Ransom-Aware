package ransomaware;

import java.io.File;

public class RansomAware {

    public RansomAware(String path, int port, boolean firstTime) {
        if (!firstTime) {
            // validateFS()
        }

        Server.start(this, port);
    }

    private void firstTimeSetup() {
        File file = new File(ServerVariables.FS_PATH);
        boolean success = file.mkdir();
        if (success) {
            System.out.println("Created fs folder successfully");
        }
    }
}

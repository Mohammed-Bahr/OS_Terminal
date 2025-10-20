import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

@SuppressWarnings("unused")
public class MyTerminal {
    // current working directory for this terminal instance
    private Path currentDirectory;

    public MyTerminal() {
        this.currentDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    // --- ls list all files / flders in currect directory ---
    // Function: ls -> list in order, ls -r -> list in reverse order
    public void ls(String[] args) {
        try {
            // arguments check 
            boolean reverse = false;
            if (args != null && args.length > 0) {
                if (args.length > 1) {
                    System.out.println("ls: too many arguments (currently only supports one argument)");
                    return;
                }
                if (!args[0].equals("-r")) {
                    System.out.println("ls: invalid argument (currently only supports -r)");
                    return;
                } else {
                    reverse = true;
                }
            }

            Path dir = currentDirectory;

            // Like an array but does not store all elements as a whole it stores one by one
            List<String> names = new ArrayList<>();

            // Convert the directory stream to an array of Paths
            Path[] paths = Files.list(dir).toArray(Path[]::new);

            // take the file name from the whole directory and convert it to string using normal for loop
            for (int i = 0; i < paths.length; i++) {
                try {
                    if (Files.isReadable(paths[i])) // ensure the file is readable
                        names.add(paths[i].getFileName().toString());
                    } catch (Exception e) {
                    // skip files that can't be accessed
                }
            }

            // sort reversely if -r is provided
            if (reverse) {
                names.sort(Comparator.reverseOrder());
            } else {
                names.sort(Comparator.naturalOrder());
            }

            // print files 
            for (int i = 0; i < names.size(); i++) {
                System.out.println(names.get(i));
            }

            // if directory not found or can't be accessed
            } catch (Exception e) {
                System.out.println("ls: cannot access directory " + currentDirectory);
            }
        }

        

    // --- CD change directory ---
    // Function: cd() -> go to home, cd("..") go back one step, cd("targetPath")
    public void cd(String[] args) {
        // arguments check
        if (args != null && args.length > 1) {
            System.out.println("cd: too many arguments");
            return;
        }

        // Handle Case #1 : no arguments (go to home directory)
        if (args == null || args.length == 0) {
            Path home = Paths.get(System.getProperty("user.home"));
            // Absolute path is a the full path from root to the target but the relative path is from current directory to target
            // Set currentDirectory to the absolute and normalized (no redundancy) path of home
            // toAbsolutePath() : It converts any relative path into an absolute path that starts from the root of the file system.
            currentDirectory = home.toAbsolutePath().normalize();
            return;
        }

        // Handle Case #2 : one argument = ..
        String target = args[0];
        if (target.equals("..")) {
            Path parent = currentDirectory.getParent();
            if (parent != null) currentDirectory = parent.normalize();
            // if parent is null (root), just keep current
            return;
        }

        // Handle Case #3 : one argument = targetPath
        try {
            Path temp = Paths.get(target);
            Path final_path;

            if (temp.isAbsolute()) {
                final_path = temp;
            } else {
                // resolve combines currentDirectory with temp to form a new path
                final_path = currentDirectory.resolve(temp);
            }
            final_path = final_path.toAbsolutePath().normalize();

            if (Files.exists(final_path) && Files.isDirectory(final_path)) {
                currentDirectory = final_path;
            } else {
                System.out.println("cd: cannot change directory '" + target + "': No such directory");
            }
        } catch (InvalidPathException e) {
            System.out.println("cd: failed to change directory '" + target + "': Invalid path");
        }
    }

    // --- rm Remove ---
    // Function: Takes 1 argument which is a file name that exists in the current directory and removes this file.
    public void rm(String[] args) {
        // arguments check
        if (args == null || args.length != 1) {
            System.out.println("rm: Invalid number of arguments (currently only supports one file)");
            return;
        }
        
        String name = args[0];
        try {
            Path target = Paths.get(name);
            Path file_Path;

            // check if the file's target path is absolute or relative
            if (target.isAbsolute()) {
                file_Path = target;
            } else {
                file_Path = currentDirectory.resolve(target);
            }

            if (!Files.exists(file_Path)) {
                System.out.println("rm: cannot remove '" + name + "': No such file or directory");
                return;
            }
            // check if it's a regular file not directory or other type
            if (!Files.isRegularFile(file_Path)) {
                System.out.println("rm: cannot remove '" + name + "': Not a regular file");
                return;
            }

            Files.delete(file_Path);
        } catch (NoSuchFileException e) {
            System.out.println("rm: cannot remove '" + name + "': No such file or directory");
        } catch (SecurityException e) {
            System.out.println("rm: cannot remove '" + name + "': Permission denied");
        } catch (InvalidPathException e) {
            System.out.println("rm: failed to remove '" + name + "': Invalid path");
        } catch (IOException e) {
            System.out.println("rm: cannot remove '" + name + "': " + e.getMessage());
        }
    }

    // --- CP copy ---
    // Function: cp from source to destination (2 arguments)     (file to file)
    public void cp(String[] args) {
    // Check arguments
    if (args == null || args.length > 2) {
        System.out.println("cp: usage: cp <source> <destination>");
        return;
    }

    String source_Str = args[0];
    String destination_Str = args[1];

    try {
        Path src = Paths.get(source_Str);
        Path dest = Paths.get(destination_Str);

        // Make paths absolute -> relative to currentDirectory if needed
        if (!src.isAbsolute()) {
            src = currentDirectory.resolve(src);
        }
        if (!dest.isAbsolute())
        {
            dest = currentDirectory.resolve(dest);
        } 
        src = src.toAbsolutePath().normalize();
        dest = dest.toAbsolutePath().normalize();

        // Check that source exists and is a regular file
        if (!Files.exists(src) || !Files.isRegularFile(src)) {
            System.out.println("cp: failed to copy '" + source_Str + "': No such file or not a regular file");
            return;
        }

        // Ensure parent directory of destination exists
        Path parent = dest.getParent();
        if (parent != null && !Files.exists(parent)) {
            System.out.println("cp: failed to copy '" + source_Str + "': Destination directory does not exist");
            return;
        }

        // Copy file, overwrite if destination exists
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

    } catch (InvalidPathException e) {
        System.out.println("cp: failed to copy '" + source_Str + "': Invalid path");
    } catch (NoSuchFileException e) {
        System.out.println("cp: failed to copy '" + source_Str + "': No such file or directory");
    } catch (SecurityException e) {
        System.out.println("cp: failed to copy '" + source_Str + "': Permission denied");
    } catch (IOException e) {
        System.out.println("cp: failed to copy '" + source_Str + "': " + e.getMessage());
    }
}

    // --- ZIP compress ---
    // Function: zip <archive.zip> <file1> <file2> ... - Compress files into a ZIP archive
    public void zip(String[] args) {
        if (args == null || args.length < 2) { // as archive.zip will be the first argument , and second will be the first file 
            //so we need at least 2 arguments   
            System.out.println("zip: usage: zip <archive.zip> <file1> [file2] ...");
            return;
        }

        String archiveName = args[0]; // first argument is the archive name
        try {
            Path archivePath = Paths.get(archiveName); // this convert the string to real path to use it later
            // this section makes the archivePath absolute if it is relative
            // which is adding the archivepath to the end of the current directory path then path it to archivePath again
            if (!archivePath.isAbsolute()) {
                archivePath = currentDirectory.resolve(archivePath);
            }
            //if it were already absolute nothing will change but if it were relative we make it absolute by resolving it against currentDirectory
            archivePath = archivePath.toAbsolutePath().normalize(); //.normalize(): This method cleans up the path by removing redundant elements.

            // Create ZIP output stream
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archivePath.toFile()))) {
                // Add each file to the archive
                for (int i = 1; i < args.length; i++) {
                    String fileName = args[i];
                    Path filePath = Paths.get(fileName);
                    
                    // doing the same as we did ubove to make the filePath absolute if it is relative
                    if (!filePath.isAbsolute()) {
                        filePath = currentDirectory.resolve(filePath);
                    }
                    filePath = filePath.toAbsolutePath().normalize();

                    // this for error handling 
                    // actually we could use try and catch for each file addition but this way is more efficient and cleaner
                    if (!Files.exists(filePath)) {
                        System.out.println("zip: warning: '" + fileName + "' not found, skipping");
                        continue;
                    }

                    if (!Files.isRegularFile(filePath)) {
                        System.out.println("zip: warning: '" + fileName + "' is not a regular file, skipping");
                        continue;
                    }

                    // Add file to ZIP
                    ZipEntry zipEntry = new ZipEntry(filePath.getFileName().toString());
                    zos.putNextEntry(zipEntry);

                    // Copy file content
                    Files.copy(filePath, zos);
                    zos.closeEntry();
                    System.out.println("  adding: " + fileName);
                }
                System.out.println("zip: created archive '" + archiveName + "'");
            }

        } catch (InvalidPathException e) {
            System.out.println("zip: failed to create archive '" + archiveName + "': Invalid path");
        } catch (SecurityException e) {
            System.out.println("zip: failed to create archive '" + archiveName + "': Permission denied");
        } catch (IOException e) {
            System.out.println("zip: failed to create archive '" + archiveName + "': " + e.getMessage());
        }
    }

    // --- UNZIP extract ---
    // Function: unzip <archive.zip> [destination] - Extract files from a ZIP archive
    public void unzip(String[] args) {
        if (args == null || args.length < 1 || args.length > 2) {
            System.out.println("unzip: usage: unzip <archive.zip> [destination]");
            return;
        }

        String archiveName = args[0];
        String destStr = args.length == 2 ? args[1] : ".";

        try {
            Path archivePath = Paths.get(archiveName);
            if (!archivePath.isAbsolute()) {
                archivePath = currentDirectory.resolve(archivePath);
            }
            archivePath = archivePath.toAbsolutePath().normalize();

            Path destPath = Paths.get(destStr);
            if (!destPath.isAbsolute()) {
                destPath = currentDirectory.resolve(destPath);
            }
            destPath = destPath.toAbsolutePath().normalize();

            if (!Files.exists(archivePath)) {
                System.out.println("unzip: cannot find archive '" + archiveName + "'");
                return;
            }

            if (!Files.isRegularFile(archivePath)) {
                System.out.println("unzip: '" + archiveName + "' is not a regular file");
                return;
            }

            // Create destination directory if it doesn't exist
            if (!Files.exists(destPath)) {
                Files.createDirectories(destPath);
            }

            // Extract ZIP archive
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivePath.toFile()))) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    Path extractPath = destPath.resolve(zipEntry.getName()).normalize();
                    
                    // Security check: prevent path traversal attacks
                    if (!extractPath.startsWith(destPath)) {
                        System.out.println("unzip: warning: skipping potentially malicious entry: " + zipEntry.getName());
                        continue;
                    }

                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(extractPath);
                    } else {
                        // Ensure parent directory exists
                        Path parent = extractPath.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }

                        // Extract file
                        Files.copy(zis, extractPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("  extracting: " + zipEntry.getName());
                    }
                    zis.closeEntry();
                }
                System.out.println("unzip: extracted archive '" + archiveName + "' to '" + destStr + "'");
            }

        } catch (InvalidPathException e) {
            System.out.println("unzip: failed to extract archive '" + archiveName + "': Invalid path");
        } catch (SecurityException e) {
            System.out.println("unzip: failed to extract archive '" + archiveName + "': Permission denied");
        } catch (IOException e) {
            System.out.println("unzip: failed to extract archive '" + archiveName + "': " + e.getMessage());
        }
    }

    // getter for currentDirectory (so caller/view can show prompt)
    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    // optional: set currentDirectory (used by tests)
    public void setCurrentDirectory(Path p) {
        if (p != null) currentDirectory = p.toAbsolutePath().normalize();
    }

    public static void main(String[] args) {
    MyTerminal terminal = new MyTerminal();
    Scanner scanner = new Scanner(System.in);
    
    System.out.println("=== Terminal Testing ===");
    System.out.println("Current directory: " + terminal.getCurrentDirectory());
    System.out.println();
    
    while (true) {
        // Show current directory prompt
        System.out.print(terminal.getCurrentDirectory().getFileName() + "> ");
        String input = scanner.nextLine().trim();
        
        if (input.equals("exit")) {
            System.out.println("Goodbye!");
            break;
        }
        
        if (input.isEmpty()) {
            continue;
        }
        
        // Split command and arguments
        String[] parts = input.split("\\s+");
        String command = parts[0];
        String[] commandArgs = parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : null;
        
        try {
            switch (command) {
                case "ls":
                    terminal.ls(commandArgs);
                    break;
                    
                case "cd":
                    terminal.cd(commandArgs);
                    System.out.println("Now in: " + terminal.getCurrentDirectory());
                    break;
                    
                case "rm":
                    terminal.rm(commandArgs);
                    break;
                    
                case "cp":
                    terminal.cp(commandArgs);
                    break;
                    
                case "zip":
                    terminal.zip(commandArgs);
                    break;
                    
                case "unzip":
                    terminal.unzip(commandArgs);
                    break;
                    
                case "pwd":
                    System.out.println(terminal.getCurrentDirectory());
                    break;
                    
                case "help":
                    printHelp();
                    break;
                    
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Type 'help' for available commands");
            }
        } catch (Exception e) {
            System.out.println("Error executing command: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    scanner.close();
}

private static void printHelp() {
    System.out.println("Available commands:");
    System.out.println("  ls           - List files in current directory");
    System.out.println("  ls -r        - List files in reverse order");
    System.out.println("  cd [dir]     - Change directory (cd .. to go back)");
    System.out.println("  rm <file>    - Remove a file");
    System.out.println("  cp <src> <dest> - Copy a file");
    System.out.println("  zip <archive.zip> <file1> [file2] ... - Compress files");
    System.out.println("  unzip <archive.zip> [dest] - Extract archive");
    System.out.println("  pwd          - Show current directory");
    System.out.println("  help         - Show this help");
    System.out.println("  exit         - Exit the terminal");
}
    
}
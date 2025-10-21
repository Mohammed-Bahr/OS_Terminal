import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

// @SuppressWarnings("unused")
public class Terminal {
    // current working directory for this terminal instance
    private Path currentDirectory;

    public Terminal() {
        this.currentDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    // --- ls list all files / folders in current directory ---
    public void ls(String[] args) {
        try {
            // argument check
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

            // Like an array but doesn't store all elements as a whole it stores one by one
            List<String> names = new ArrayList<>();
            // Convert the directory stream to an array of paths
            Path[] paths = Files.list(dir).toArray(Path[]::new);
            // Take the file name from the whole directory and convert it to string using
            // normal for loop
            for (int i = 0; i < paths.length; i++) {
                try {
                    if (Files.isReadable(paths[i])) // Ensure the file is readble
                        names.add(paths[i].getFileName().toString());
                } catch (Exception e) {
                }
                // skip files that cant be accessed
            }
            // sort reversely if -r is provided
            if (reverse)
                names.sort(Comparator.reverseOrder());
            else
                names.sort(Comparator.naturalOrder());

            // print files
            for (String name : names)
                System.out.println(name);

        } catch (Exception e) { // if directory not founr or cant be accessed
            System.out.println("ls: cannot access directory " + currentDirectory);
        }
    }

    // --- CD change directory ---
    public void cd(String[] args) {
        if (args != null && args.length > 1) {
            System.out.println("cd: too many arguments");
            return;
        }

        if (args == null || args.length == 0) {
            Path home = Paths.get(System.getProperty("user.home"));
            currentDirectory = home.toAbsolutePath().normalize();
            return;
        }

        String target = args[0];
        if (target.equals("..")) {
            Path parent = currentDirectory.getParent();
            if (parent != null)
                currentDirectory = parent.normalize();
            return;
        }

        try {
            Path temp = Paths.get(target);
            Path final_path;

            if (temp.isAbsolute())
                final_path = temp;
            else
                final_path = currentDirectory.resolve(temp);

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
    public void rm(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("rm: Invalid number of arguments (currently only supports one file)");
            return;
        }

        String name = args[0];
        try {
            Path target = Paths.get(name);
            Path file_Path;

            if (target.isAbsolute())
                file_Path = target;
            else
                file_Path = currentDirectory.resolve(target);

            if (!Files.exists(file_Path)) {
                System.out.println("rm: cannot remove '" + name + "': No such file or directory");
                return;
            }
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

    // --- CP copy (file-to-file) ---
    public void cp(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("cp: usage: cp <source> <destination>");
            return;
        }

        String source_Str = args[0];
        String destination_Str = args[1];

        try {
            Path src = Paths.get(source_Str);
            Path dest = Paths.get(destination_Str);

            if (!src.isAbsolute())
                src = currentDirectory.resolve(src);
            if (!dest.isAbsolute())
                dest = currentDirectory.resolve(dest);

            src = src.toAbsolutePath().normalize();
            dest = dest.toAbsolutePath().normalize();

            if (!Files.exists(src) || !Files.isRegularFile(src)) {
                System.out.println("cp: failed to copy '" + source_Str + "': No such file or not a regular file");
                return;
            }

            Path parent = dest.getParent();
            if (parent != null && !Files.exists(parent)) {
                System.out.println("cp: failed to copy '" + source_Str + "': Destination directory does not exist");
                return;
            }

            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

        } catch (Exception e) {
            System.out.println("cp: failed to copy '" + source_Str + "': " + e.getMessage());
        }
    }

    // ---------- mkdir ----------
    public void mkdir(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("mkdir: missing directory name(s)");
            return;
        }

        for (String arg : args) {
            try {
                Path dirPath = currentDirectory.resolve(arg).normalize();
                if (Files.exists(dirPath)) {
                    System.out.println("mkdir: Directory already exists: " + dirPath.getFileName());
                } else {
                    Files.createDirectories(dirPath);
                    System.out.println("Directory created: " + dirPath.getFileName());
                }
            } catch (Exception e) {
                System.out.println("mkdir: error creating directory '" + arg + "': " + e.getMessage());
            }
        }
    }

    // ---------- rmdir ----------
    public void rmdir(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("rmdir: usage: rmdir <dirname> or rmdir *");
            return;
        }

        String target = args[0];

        if (target.equals("*")) {
            try (Stream<Path> paths = Files.list(currentDirectory)) {
                paths.forEach(path -> {
                    if (Files.isDirectory(path)) {
                        try (Stream<Path> sub = Files.list(path)) {
                            if (sub.findAny().isEmpty()) {
                                Files.delete(path);
                                System.out.println("Removed empty directory: " + path.getFileName());
                            }
                        } catch (IOException e) {
                            System.out.println("rmdir: cannot delete " + path.getFileName());
                        }
                    }
                });
            } catch (IOException e) {
                System.out.println("rmdir: error reading current directory");
            }
        } else {
            try {
                Path dirPath = currentDirectory.resolve(target).normalize();
                if (!Files.exists(dirPath)) {
                    System.out.println("rmdir: directory not found: " + dirPath.getFileName());
                    return;
                }
                if (!Files.isDirectory(dirPath)) {
                    System.out.println("rmdir: not a directory: " + dirPath.getFileName());
                    return;
                }
                try (Stream<Path> contents = Files.list(dirPath)) {
                    if (contents.findAny().isEmpty()) {
                        Files.delete(dirPath);
                        System.out.println("Directory removed: " + dirPath.getFileName());
                    } else {
                        System.out.println("rmdir: directory not empty: " + dirPath.getFileName());
                    }
                }
            } catch (Exception e) {
                System.out.println("rmdir: error removing directory: " + e.getMessage());
            }
        }
    }

    // ---------- cp -r ----------
    public void cp_r(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("cp -r: usage: cp -r <sourceDir> <destinationDir>");
            return;
        }

        Path source = currentDirectory.resolve(args[0]).normalize();
        Path destination = currentDirectory.resolve(args[1]).normalize();

        if (!Files.exists(source) || !Files.isDirectory(source)) {
            System.out.println("cp -r: source directory does not exist or is not a directory: " + source.getFileName());
            return;
        }

        try {
            if (!Files.exists(destination))
                Files.createDirectories(destination);

            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = destination.resolve(source.relativize(dir));
                    if (!Files.exists(targetDir))
                        Files.createDirectories(targetDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFile = destination.resolve(source.relativize(file));
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });

            System.out.println(
                    "Copied directory recursively from " + source.getFileName() + " to " + destination.getFileName());
        } catch (Exception e) {
            System.out.println("cp -r: error copying directory: " + e.getMessage());
        }
    }

    // ---------- cat ----------
    public void cat(String[] args) {
        if (args == null || (args.length != 1 && args.length != 2)) {
            System.out.println(
                    "Oops! incorrect file name. Please use format: cat filename.extension or cat file1name.extension file2name.extension ....");
            return;
        }

        for (String file_Name : args) {
            Path file_Path = currentDirectory.resolve(file_Name).normalize();
            if (!Files.exists(file_Path) || !Files.isRegularFile(file_Path)) {
                System.out.println("\"" + file_Name + "\" No such file or the file not found!");
                return;
            }

            try {
                List<String> All_Lines = Files.readAllLines(file_Path);
                for (String line : All_Lines) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                System.out.println("cat: error reading " + file_Name);
            }
        }
    }

    // ---------- wc ----------
    public void wc(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("Oops! incorrect file name. Please use format: wc filename.extension");
            return;
        }

        String fileName = args[0];
        Path filePath = currentDirectory.resolve(fileName).normalize();

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            System.out.println("\"" + fileName + "\" No such file or the file not found!");
            return;
        }

        try {
            List<String> All_Lines = Files.readAllLines(filePath);
            int line_Count = All_Lines.size();
            int word_Count = 0;
            int char_Count = 0;

            for (String line : All_Lines) {
                if (line.trim().isEmpty() == false) {
                    word_Count += line.trim().split("\\s+").length;
                } else {
                    word_Count += 0;
                }
                char_Count += line.length() + 1;
            }

            System.out.println(line_Count + " " + word_Count + " " + char_Count + " " + fileName);
        } catch (IOException e) {
            System.out.println("wc: error reading " + fileName);
        }
    }

    // ---------- zip ----------
    public void zip(String[] args) {

        boolean recursive = false;
        int startIndex = 0;

        if (args[0].equals("-r")) {
            recursive = true;
            startIndex = 1;
            if (args.length < 3) {
                System.out.println("zip: usage: zip -r <archive.zip> <directory>");
                return;
            }
        } else {
            if (args == null || args.length < 2) {
                System.out.println("zip: usage: zip [-r] <archive.zip> <file1> [file2] ...");
                return;
            }
        }

        String archiveName = args[startIndex];
        try {
            Path archivePath = Paths.get(archiveName);
            if (!archivePath.isAbsolute()) {
                archivePath = currentDirectory.resolve(archivePath);
            }
            archivePath = archivePath.toAbsolutePath().normalize();

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archivePath.toFile()))) {
                for (int i = startIndex + 1; i < args.length; i++) {
                    Path filePath = Paths.get(args[i]);
                    if (!filePath.isAbsolute()) {
                        filePath = currentDirectory.resolve(filePath);
                    }
                    filePath = filePath.toAbsolutePath().normalize();

                    if (!Files.exists(filePath)) {
                        System.out.println("zip: warning: '" + filePath + "' not found, So we will skipping it -> ");
                        continue;
                    }

                    if (recursive && Files.isDirectory(filePath)) {
                        final Path finalFilePath = filePath;
                        Files.walk(filePath).forEach(path -> {
                            try {
                                if (!Files.isDirectory(path)) {
                                    ZipEntry entry = new ZipEntry(finalFilePath.relativize(path).toString());
                                    zos.putNextEntry(entry);
                                    Files.copy(path, zos);
                                    zos.closeEntry();
                                    System.out.println("  adding: " + path);
                                }
                            } catch (IOException e) {
                                System.out.println("zip: error adding " + path + " -> " + e.getMessage());
                            }
                        });
                    } else if (Files.isRegularFile(filePath)) {
                        // ضغط ملف عادي
                        ZipEntry entry = new ZipEntry(filePath.getFileName().toString());
                        zos.putNextEntry(entry);
                        Files.copy(filePath, zos);
                        zos.closeEntry();
                        System.out.println("  adding: " + filePath);
                    } else {
                        System.out.println("zip: warning: '" + filePath + "' is not a regular file, skipping");
                    }
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

    // ---------- unzip ----------
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

            if (!Files.exists(destPath)) {
                Files.createDirectories(destPath);
            }

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivePath.toFile()))) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    Path extractPath = destPath.resolve(zipEntry.getName()).normalize();

                    if (!extractPath.startsWith(destPath)) {
                        System.out
                                .println("unzip: warning: skipping potentially malicious entry: " + zipEntry.getName());
                        continue;
                    }

                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(extractPath);
                    } else {
                        Path parent = extractPath.getParent();
                        if (parent != null && !Files.exists(parent)) {
                            Files.createDirectories(parent);
                        }

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

    // getter for currentDirectory
    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentDirectory(Path p) {
        if (p != null)
            currentDirectory = p.toAbsolutePath().normalize();
    }

    public static void main(String[] args) {
        Terminal terminal = new Terminal();
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Terminal Testing ===");
        System.out.println("Current directory: " + terminal.getCurrentDirectory());
        System.out.println();

        while (true) {
            System.out.print(terminal.getCurrentDirectory().getFileName() + "> ");
            String input = scanner.nextLine().trim();

            if (input.equals("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            if (input.isEmpty())
                continue;

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
                        // handle "cp -r src dest"
                        if (commandArgs != null && commandArgs.length > 0 && commandArgs[0].equals("-r")) {
                            String[] newArgs = Arrays.copyOfRange(commandArgs, 1, commandArgs.length);
                            terminal.cp_r(newArgs);
                        } else {
                            terminal.cp(commandArgs);
                        }
                        break;

                    case "mkdir":
                        terminal.mkdir(commandArgs);
                        break;

                    case "rmdir":
                        terminal.rmdir(commandArgs);
                        break;

                    case "pwd":
                        System.out.println(terminal.getCurrentDirectory());
                        break;

                    case "cat":
                        terminal.cat(commandArgs);
                        break;

                    case "wc":
                        terminal.wc(commandArgs);
                        break;

                    case "zip":
                        terminal.zip(commandArgs);
                        break;
                    case "unzip":
                        terminal.unzip(commandArgs);
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
        System.out.println("  ls              - List files in current directory");
        System.out.println("  ls -r           - List files in reverse order");
        System.out.println("  cd [dir]        - Change directory (cd .. to go back)");
        System.out.println("  mkdir <dir...>  - Create one or more directories");
        System.out.println("  rmdir <dir|*>   - Remove an empty directory or all empty ones");
        System.out.println("  cp <src> <dest> - Copy a file");
        System.out.println("  cp -r <src> <dest> - Copy directory recursively");
        System.out.println("  rm <file>       - Remove a file");
        System.out.println("  pwd             - Show current directory");
        System.out.println("  wc              - Count number of lines and words in the file with the file name added");
        System.out.println(
                "  cat             - Print the file’s content or concatenates the content of the 2 files and prints it");
        System.out.println("  help            - Show this help");
        System.out.println("  exit            - Exit the terminal");
    }
}

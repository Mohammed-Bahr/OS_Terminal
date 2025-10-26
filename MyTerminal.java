import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Terminal {
    class Parser {
        private String fullCommand;
        private String commandName;
        private Vector<String> args = new Vector<String>();

        public boolean parse(String input) {
            if (input.isEmpty()) {
                return false;
            } else {
                fullCommand = input;
                Pattern pattern = Pattern.compile("\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)?\"|(?:\\S+)");
                Matcher matcher = pattern.matcher(input);

                while (matcher.find()) {
                    args.add(matcher.group());
                }

                if (args.size() == 1) {
                    commandName = args.get(0);
                    args.remove(0);
                } else {
                    if (args.size() >= 2 && args.get(1).equals("-r")) {
                        commandName = args.get(0) + " " + args.get(1);
                        args.remove(0);
                        args.remove(0);
                    } else {
                        commandName = args.get(0);
                        args.remove(0);
                    }
                }
                commandName = commandName.toLowerCase();
                return true;
            }
        }

        public String getFullCommand() {
            return fullCommand;
        }

        public String getCommandName() {
            return commandName;
        }

        public Vector<String> getArgs() {
            return args;
        }

        public void clearFullCommand() {
            fullCommand = "";
        }

        public void clearCommandName() {
            commandName = "";
        }

        public void clearArgs() {
            args.clear();
        }
    }

    private Path currentDirectory;
    private Parser parser = new Parser();

    public Terminal() {
        this.currentDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    public void open() {
        Scanner input = new Scanner(System.in);
        while (true) {
            String prompt = currentDirectory.getFileName() != null ? currentDirectory.getFileName().toString()
                    : currentDirectory.toString();
            System.out.print(prompt + " > ");
            String userInput = input.nextLine();
            if (userInput.equals("exit")) {
                System.out.println("Goodbye!");
                break;
            }
            if (parser.parse(userInput)) {
                this.chooseCommandAction(parser);
            }
            parser.clearArgs();
            parser.clearCommandName();
            parser.clearFullCommand();
        }
        input.close();
    }

    public void chooseCommandAction(Parser parser) {
        Vector<String> args = new Vector<>(parser.getArgs());
        String outputFile = null;
        boolean append = false;

        int redirectIndex = -1;
        for (int i = 0; i < args.size(); i++) {
            if (args.get(i).equals(">") || args.get(i).equals(">>")) {
                redirectIndex = i;
                append = args.get(i).equals(">>");
                break;
            }
        }

        if (redirectIndex != -1 && redirectIndex < args.size() - 1) {
            outputFile = args.get(redirectIndex + 1);

            args.remove(redirectIndex);
            args.remove(redirectIndex);
        }

        String[] argsArray = args.toArray(new String[0]);

        if (outputFile != null) {
            executeWithRedirection(parser.getCommandName(), argsArray, outputFile, append);
        } else {
            executeCommand(parser.getCommandName(), argsArray);
        }
    }

    private void executeWithRedirection(String commandName, String[] args, String outputFile, boolean append) {
        PrintStream originalOut = System.out;

        try {
            if (outputFile.length() >= 2 &&
                    (outputFile.charAt(0) == '"') &&
                    (outputFile.charAt(outputFile.length() - 1) == '"')) {
                outputFile = outputFile.substring(1, outputFile.length() - 1);
            }

            Path outputPath = Paths.get(outputFile);
            if (!outputPath.isAbsolute()) {
                outputPath = currentDirectory.resolve(outputFile).normalize();
            }

            Path parentDir = outputPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            try (PrintStream fileOut = new PrintStream(new FileOutputStream(outputPath.toFile(), append))) {
                System.setOut(fileOut);
                executeCommand(commandName, args);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error: Cannot create output file: " + outputFile);
        } catch (IOException e) {
            System.err.println("Error: Cannot create directory for output file: " + e.getMessage());
        } finally {
            System.setOut(originalOut);
        }
    }

    private void executeCommand(String commandName, String[] args) {

        switch (commandName) {
            case "echo":
                echo(args);
                break;
            case "cd":
                cd(args);
                System.out.println("Now in: " + getCurrentDirectory());
                break;
            case "pwd":
                pwd();
                break;
            case "ls":
                ls(args);
                break;
            case "wc":
                wc(args);
                break;
            case "touch":
                touch(args);
                break;
            case "cat":
                cat(args);
                break;
            case "cp":
                cp(args);
                break;
            case "cp -r":
                cp_r(args);
                break;
            case "rm":
                rm(args);
                break;
            case "mkdir":
                mkdir(args);
                break;
            case "rmdir":
                rmdir(args);
                break;
            case "zip":
                zip(args, false);
                break;
            case "zip -r":
                zip(args, true);
                break;
            case "unzip":
                unzip(args);
                break;
            case "help":
                printHelp();
                break;
            default:
                System.out.println("Unknown command: " + commandName);
                System.out.println("Type 'help' for available commands");
        }
    }

    public void echo(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("echo: missing arguments");
        } else {
            for (int i = 0; i < args.length; i++) {
                System.out.print("\n" + args[i]);
                if (i < args.length - 1)
                    System.out.print(" ");
            }
            System.out.println();
        }
    }

    public void ls(String[] args) {
        try {
            if (args.length > 0) {
                System.out.println("ls: takes no arguments");
                return;
            }
            Path dir = currentDirectory;
            List<String> names = new ArrayList<>();
            Path[] paths = Files.list(dir).toArray(Path[]::new);

            for (Path path : paths) {
                try {
                    if (Files.isReadable(path))
                        names.add(path.getFileName().toString());
                } catch (Exception e) {

                }
            }
            names.sort(Comparator.naturalOrder());

            for (String name : names) {
                System.out.println(name);
            }

        } catch (Exception e) {
            System.out.println("ls: cannot access directory " + currentDirectory);
        }
    }

    // --- CD change directory ---
    public void cd(String[] args) {
        if (args != null && args.length > 1) {
            System.out.println("cd: takes 1 or no arguments");
            return;
        }

        if (args == null || args.length == 0) {
            Path home = Paths.get(System.getProperty("user.home"));
            currentDirectory = home.toAbsolutePath().normalize();
            return;
        }

        String target = args[0];

        if (target.length() >= 2 &&
                (target.charAt(0) == '"') &&
                (target.charAt(target.length() - 1) == '"')) {
            target = target.substring(1, target.length() - 1);
        }

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

            if (final_path.equals(currentDirectory)) {
                System.out.println("Already in: " + currentDirectory);
                return;
            }

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
            System.out.println("rm: must have 1 argument which is file name in current directory");
            return;
        }

        String name = args[0];
        if (name.length() >= 2 &&
                (name.charAt(0) == '"') &&
                (name.charAt(name.length() - 1) == '"')) {
            name = name.substring(1, name.length() - 1);
        }

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
            if (Files.isDirectory(file_Path)) {
                System.out.println("Cannot delete directory: " + name);
                return;
            }
            if (!Files.isRegularFile(file_Path)) {
                System.out.println("rm: cannot remove '" + name + "': Not a regular file");
                return;
            }

            Files.delete(file_Path);
        } catch (NoSuchFileException e) {
            System.out.println("rm: cannot remove '" + name + "': No such file or directory");
        } catch (InvalidPathException e) {
            System.out.println("rm: failed to remove '" + name + "': Invalid path");
        } catch (IOException e) {
            System.out.println("rm: cannot remove '" + name + "': " + e.getMessage());
        }
    }

    // --- CP ------
    public void cp(String[] args) {
        if (args == null || args.length != 2) {
            System.out.println("cp: takes 2 arguments cp <source> <destination>");
            return;
        }

        String source_Str = args[0];
        String destination_Str = args[1];

        if ((source_Str.charAt(0) == '"') && (source_Str.charAt(source_Str.length() - 1) == '"')) {
            source_Str = source_Str.substring(1, source_Str.length() - 1);
        }

        if ((destination_Str.charAt(0) == '"') && (destination_Str.charAt(destination_Str.length() - 1) == '"')) {
            destination_Str = destination_Str.substring(1, destination_Str.length() - 1);
        }

        try {
            Path src = Paths.get(source_Str);
            Path dest = Paths.get(destination_Str);

            if (!src.isAbsolute())
                src = currentDirectory.resolve(src);
            if (!dest.isAbsolute())
                dest = currentDirectory.resolve(dest);

            src = src.toAbsolutePath().normalize();
            dest = dest.toAbsolutePath().normalize();

            if (!Files.exists(src)) {
                System.out.println("Source file does not exist: " + source_Str);
                return;
            }

            if (!Files.isRegularFile(src)) {
                System.out.println("cp: cannot copy '" + source_Str + "': Not a regular file");
                return;
            }

            if (!Files.exists(dest)) {
                System.out.println("Destination file does not exist: " + destination_Str);
                return;
            }

            if (!Files.isWritable(dest)) {
                System.out.println("Destination file is not writable: " + destination_Str);
                return;
            }

            if (Files.isSameFile(src, dest)) {
                System.out.println("Source and destination files are the same");
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
            String fullpath = arg;
            if (fullpath.length() >= 2 &&
                    (fullpath.charAt(0) == '"') &&
                    (fullpath.charAt(fullpath.length() - 1) == '"')) {
                fullpath = fullpath.substring(1, fullpath.length() - 1);
            }
            try {
                Path dirPath = currentDirectory.resolve(fullpath).normalize();
                if (Files.exists(dirPath)) {
                    System.out.println("mkdir: Directory already exists: " + dirPath.getFileName());
                    continue;
                }
                Path parentDir = dirPath.getParent();
                if (parentDir != null && (!Files.exists(parentDir) || !Files.isWritable(parentDir))) {
                    System.out.println("Parent directory does not exist or is not writable: " + fullpath);
                    continue;
                }
                Files.createDirectories(dirPath);
                System.out.println("Directory created: " + dirPath.getFileName());

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
        if (target.length() >= 2 &&
                (target.charAt(0) == '"') &&
                (target.charAt(target.length() - 1) == '"')) {
            target = target.substring(1, target.length() - 1);
        }

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

        String sourcePath = args[0];
        String destinationPath = args[1];

        if ((sourcePath.charAt(0) == '"') && (sourcePath.charAt(sourcePath.length() - 1) == '"')) {
            sourcePath = sourcePath.substring(1, sourcePath.length() - 1);
        }

        if ((destinationPath.charAt(0) == '"') && (destinationPath.charAt(destinationPath.length() - 1) == '"')) {
            destinationPath = destinationPath.substring(1, destinationPath.length() - 1);
        }

        Path source = currentDirectory.resolve(sourcePath).normalize();
        Path destination = currentDirectory.resolve(destinationPath).normalize();

        if (!Files.exists(source) || !Files.isDirectory(source)) {
            System.out.println("cp -r: source directory does not exist or is not a directory: " + source.getFileName());
            return;
        }

        File destinationDir = destination.toFile();

        if (!destinationDir.exists() || !destinationDir.isDirectory()) {
            System.out.println("cp -r: destination directory does not exist: " + destination.getFileName());
            return;
        }

        if (!destinationDir.canWrite()) {
            System.out.println("cp -r: destination directory is not writable: " + destination.getFileName());
            return;
        }

        try {
            if (Files.isSameFile(source, destination)) {
                System.out.println("cp -r: source and destination directories are the same");
                return;
            }
        } catch (IOException e) {
            System.out.println("cp -r: error comparing directories: " + e.getMessage());
            return;
        }

        try {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDir = destination.resolve(source.relativize(dir));
                    if (!Files.exists(targetDir)) {
                        Files.createDirectories(targetDir);
                    }
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
                    "incorrect file name. Please use format: cat filename.extension or cat file1name.extension file2name.extension ....");
            return;
        }

        for (String file_Name : args) {
            if (file_Name.length() >= 2 &&
                    (file_Name.charAt(0) == '"') &&
                    (file_Name.charAt(file_Name.length() - 1) == '"')) {
                file_Name = file_Name.substring(1, file_Name.length() - 1);
            }
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
                System.out.println("---------- End of " + file_Name + " ---------");
            } catch (IOException e) {
                System.out.println("cat: error reading " + file_Name);
            }
        }
    }

    // ---------- wc ----------
    public void wc(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("incorrect file name. Please use format: wc filename.extension");
            return;
        }

        String fileName = args[0];

        if ((fileName.charAt(0) == '"') && (fileName.charAt(fileName.length() - 1) == '"')) {
            fileName = fileName.substring(1, fileName.length() - 1);
        }

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
                if (!line.trim().isEmpty()) {
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

    // ------------- zip---------------
    public void zip(String[] args, boolean recursive) {
        if (recursive) {
            if (args.length < 2) {
                System.out.println("zip: usage: zip -r <archive.zip> <directory>");
                return;
            }
        } else {
            if (args.length < 2) {
                System.out.println("zip: usage: zip <archive.zip> <file1> [file2] ...");
                return;
            }
        }

        String archiveName = args[0];
        if (archiveName.length() >= 2 &&
                (archiveName.charAt(0) == '"') &&
                (archiveName.charAt(archiveName.length() - 1) == '"')) {
            archiveName = archiveName.substring(1, archiveName.length() - 1);
        }

        if (!archiveName.endsWith(".zip")) {
            archiveName += ".zip";
        }

        try {
            Path archivePath = currentDirectory.resolve(archiveName).normalize(); // archivePath now have the absolute
                                                                                  // path of the zip file

            Path parentDir = archivePath.getParent(); // is the same as currentDirectory here
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archivePath.toFile()))) {
                boolean addedFiles = false;

                for (int i = 1; i < args.length; i++) {
                    String inputName = args[i];
                    if (inputName.length() >= 2 &&
                            (inputName.charAt(0) == '"') &&
                            (inputName.charAt(inputName.length() - 1) == '"')) {
                        inputName = inputName.substring(1, inputName.length() - 1);
                    }
                    Path inputPath = currentDirectory.resolve(inputName).normalize(); // inputPath is -> the path of the
                                                                                      // current dir + inputFolderName

                    if (!Files.exists(inputPath)) {
                        System.out.println("zip: warning: '" + inputName + "' not found, skipping");
                        continue;
                    }
                    // this for recursive part
                    if (Files.isDirectory(inputPath)) {
                        if (recursive) {
                            Files.walk(inputPath)
                                    .filter(path -> Files.isRegularFile(path))
                                    .forEach(file -> {
                                        try {
                                            Path relativePath = inputPath.relativize(file);
                                            String entryName = inputPath.getFileName().toString() + "/" +
                                                    relativePath.toString().replace("\\", "/");

                                            ZipEntry entry = new ZipEntry(entryName);
                                            zos.putNextEntry(entry);
                                            Files.copy(file, zos);
                                            zos.closeEntry();
                                            System.out.println("  adding: " + entryName);
                                        } catch (IOException e) {
                                            System.out.println("zip: error adding " + file + ": " + e.getMessage());
                                        }
                                    });
                            addedFiles = true;
                        } else {
                            System.out
                                    .println("zip: error: '" + inputName + "' is a directory (use -r for directories)");
                            return;
                        }
                    } else if (Files.isRegularFile(inputPath)) {
                        ZipEntry entry = new ZipEntry(inputPath.getFileName().toString());
                        zos.putNextEntry(entry);
                        Files.copy(inputPath, zos);
                        zos.closeEntry();
                        System.out.println("  adding: " + inputName);
                        addedFiles = true;
                    } else {
                        System.out.println(
                                "zip: warning: '" + inputName + "' is not a regular file or directory, skipping");
                    }
                }

                if (addedFiles) {
                    System.out.println("zip: created archive '" + archiveName + "'");
                } else {
                    System.out.println("zip: no files were added to archive");
                    Files.deleteIfExists(archivePath);
                }

            } catch (IOException e) {
                System.out.println("zip: failed to create archive '" + archiveName + "': " + e.getMessage());
            }

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
        if (archiveName.length() >= 2 &&
                (archiveName.charAt(0) == '"') &&
                (archiveName.charAt(archiveName.length() - 1) == '"')) {
            archiveName = archiveName.substring(1, archiveName.length() - 1);
        }

        if (!archiveName.endsWith(".zip")) {
            archiveName += ".zip";
        }

        String destStr = args.length == 2 ? args[1] : ".";
        if (destStr.length() >= 2 &&
                (destStr.charAt(0) == '"') &&
                (destStr.charAt(destStr.length() - 1) == '"')) {
            destStr = destStr.substring(1, destStr.length() - 1);
        }

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
        } catch (IOException e) {
            System.out.println("unzip: failed to extract archive '" + archiveName + "': " + e.getMessage());
        }
    }

    // ---------- pwd ----------
    public void pwd() {
        if (!parser.getArgs().isEmpty()) {
            System.out.println("Error this command takes no arguments ");
        } else {
            System.out.println(currentDirectory);
        }
    }

    // ---------- touch ----------
    public void touch(String[] args) {
        if (args == null || args.length != 1) {
            System.out.println("touch: usage: touch <filename>");
            return;
        }

        String filename = args[0];
        if ((filename.charAt(0) == '"') && (filename.charAt(filename.length() - 1) == '"')) {
            filename = filename.substring(1, filename.length() - 1);
        }

        try {
            Path filePath = currentDirectory.resolve(filename).normalize();
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                System.out.println("File created: " + filename);
            } else {
                if (Files.isDirectory(filePath)) {
                    System.out.println("Error: Cannot create a file in a directory without file name.");
                } else {
                    System.out.println("Warning: File already exists: " + filePath.getFileName());
                }
            }

        } catch (InvalidPathException e) {
            System.out.println("Error: Invalid file path ");
        } catch (IOException e) {
            System.out.println("Error: Invalid file ");
        } catch (Exception e) {
            System.out.println("touch: error creating file '" + filename + "': " + e.getMessage());
        }
    }

    private static void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  ls              - List files in current directory");
        System.out.println("  cd [dir]        - Change directory (cd .. to go back)");
        System.out.println("  mkdir <dir...>  - Create one or more directories");
        System.out.println("  rmdir <dir|*>   - Remove an empty directory or all empty ones");
        System.out.println("  cp <src> <dest> - Copy a file");
        System.out.println("  cp -r <src> <dest> - Copy directory recursively");
        System.out.println("  rm <file>       - Remove a file");
        System.out.println("  pwd             - Show current directory");
        System.out.println("  touch <fileName>        - Creates file");
        System.out.println("  Command > Filename    - Redirects output of command to a file (Replace) ");
        System.out.println("  Command >> Filename    - Redirects output of command to a file (Append) ");
        System.out.println("  wc              - Count number of lines and words in the file with the file name added");
        System.out.println(
                "  cat             - Print the file’s content or concatenates the content of the 2 files and prints it");
        System.out.println("  zip archive_name.zip file1 file2         -compress 2 files ");
        System.out.println("  zip -r archive_name.zip <dir>         -compress dir and subdirectories ");
        System.out.println("  unzip archive_name.zip              - extract all files from a .zip ");
        System.out.println("  help            - Show this help");
        System.out.println("  exit            - Exit the terminal");
    }

    public Path getCurrentDirectory() {
        return currentDirectory;
    }

    public static void main(String[] args) {
        Terminal terminal = new Terminal();
        terminal.open();
    }
}

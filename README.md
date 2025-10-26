# 🖥️ Java Terminal Emulator

A **fully functional terminal emulator** built in **Java** that replicates common Unix shell commands (e.g., `ls`, `cd`, `cp`, `rm`, `mkdir`, `zip`, `unzip`, etc.).  
It allows users to interact with the file system, execute commands, and even redirect output to files — all from a Java-based console interface.

---

## 🚀 Features

- ✅ Command parsing with argument handling and quote support  
- 📂 Directory navigation (`cd`, `ls`, `pwd`)  
- 🧱 File manipulation (`cp`, `rm`, `touch`, `cat`, `wc`)  
- 📁 Directory creation & removal (`mkdir`, `rmdir`)  
- 🗜️ Compression commands (`zip`, `unzip`, including recursive `-r` option)  
- 🔄 Output redirection (`>` and `>>`)  
- 💡 Built-in `help` command listing all available commands  
- 🧩 Safe path handling using Java NIO  
- 🧹 Exception handling for most file operations  

---

## 🧰 Implemented Commands

| Command | Description |
|----------|-------------|
| `ls` | List files and folders in the current directory |
| `cd [dir]` | Change the current working directory (`cd ..` to go back) |
| `pwd` | Print the current working directory |
| `mkdir <dir>` | Create one or more directories |
| `rmdir <dir>` | Remove an empty directory or all empty directories with `rmdir *` |
| `touch <file>` | Create a new empty file |
| `cat <file>` | Display the contents of one or more files |
| `wc <file>` | Count lines, words, and characters in a file |
| `cp <src> <dest>` | Copy a file |
| `cp -r <srcDir> <destDir>` | Copy directories recursively |
| `rm <file>` | Delete a file |
| `zip <archive.zip> <file...>` | Compress one or more files into a `.zip` archive |
| `zip -r <archive.zip> <dir>` | Recursively compress a directory |
| `unzip <archive.zip> [destDir]` | Extract a `.zip` archive |
| `echo <text>` | Print text to console (supports redirection) |
| `help` | Show all available commands |
| `exit` | Exit the terminal program |
| `>` / `>>` | Redirect output to a file (overwrite or append) |

---

## ⚙️ How to Run

### 1. Compile the Program
```bash
javac Terminal.java
```

### 2. Run the Terminal
```bash
java Terminal
```

You’ll then see a prompt like:
```
project-folder > 
```

Now you can start entering commands like:
```
pwd
ls
mkdir test
cd test
touch file.txt
echo Hello > file.txt
cat file.txt
```

---

## 📁 Example Usage

```bash
> mkdir test
> cd test
> touch hello.txt
> echo Hello, Java! > hello.txt
> cat hello.txt
Hello, Java!
> cd ..
> zip -r archive.zip test
  adding: test/hello.txt
zip: created archive 'archive.zip'
> unzip archive.zip extracted
  extracting: test/hello.txt
unzip: extracted archive 'archive.zip' to 'extracted'
```

---

## 🧱 Project Structure

```
Terminal.java
└── class Terminal
    ├── class Parser          # Handles command parsing
    ├── open()                # Starts the terminal loop
    ├── executeCommand()      # Runs a command
    ├── executeWithRedirection() # Handles output redirection
    ├── File operations: cp, rm, touch, cat, wc
    ├── Directory ops: mkdir, rmdir, cd, ls
    ├── Compression: zip, unzip
    └── Utility: help, echo, pwd
```

---

## 🧠 Design Notes

- **Uses Java NIO** (`Path`, `Files`, `StandardCopyOption`) for safe and efficient file handling.  
- **Supports quoted arguments**, e.g.:
  ```
  echo "Hello World" > "my file.txt"
  ```
- **Redirection** (`>` or `>>`) safely creates directories if missing.  
- **Recursive operations** (`cp -r`, `zip -r`) implemented with `FileVisitor`.  

---

## 🧾 License

This project is open source and available under the **MIT License**.  
You can freely use, modify, and distribute it for educational or personal use.

---

## 👤 Author

**Mohamed Ahmed Maawad Mahmoud (محمد أحمد معوض محمود)**  
Faculty of Computers and Artificial Intelligence, Cairo University  
🌍 Egypt | 💻 Java Developer | ✉️ [LinkedIn](www.linkedin.com/in/møĥãmmęđ-bãĥŗ-91716529b)

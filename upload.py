from flask import Flask, render_template, request
from zipfile import ZipFile
from waitress import serve
import os
import docx2txt

import subprocess
import time

app = Flask(__name__)

app.config["UPLOAD_PATH_txt"] = "upload"
app.config["UPLOAD_PATH_docx"] = "upload"
app.config["UPLOAD_PATH_pdf"] = "upload"
app.config["UPLOAD_PATH_zip"] = "upload"
app.config["PATH_zip"] = "upload"
# isPrivate = 'true'

@app.route("/upload_file", methods=["GET", "POST"])








def upload_file():
    flist = []
    if request.method == 'POST':
        isPrivate = request.form.get('isPrivate')
        print("checkbox isPrivate value",isPrivate)
        if(isPrivate != "true"):
            isPrivate = "false"
            print("checkbox isPrivate value",isPrivate)
        for f in request.files.getlist('file_name'):
            # f=request.files['file_name']
            #fName = f.filename
            fName = f.filename
            flist.append(fName)
            print("list of docs and zips",flist)

            if(fName.endswith(".zip")):
                f.save(os.path.join(app.config['PATH_zip'],f.filename))
                fName = app.config['PATH_zip'] + '/' + f.filename
                fpath = app.config['UPLOAD_PATH_zip']
                #print("here... fname is:", fName)
                #print("here... path is:", fpath)
                ext(fName, fpath)

            elif(fName.endswith(".txt")):
                f.save(os.path.join(app.config['UPLOAD_PATH_txt'],f.filename))
                print("saved text file name ---> ",f.filename)
            
            
            elif(fName.endswith(".docx")):
                # Convert .docx to .txt:
                MY_TEXT = docx2txt.process("test.docx")
                with open("Output.txt", "w") as text_file:
                    print(MY_TEXT, file=text_file)
                f.save(os.path.join(app.config['UPLOAD_PATH_docx'],f.filename))
                print("saved text file name ---> ",f.filename)

            # elif(fName.endswith(".pptx")):
            #     f.save(os.path.join(app.config['UPLOAD_PATH_docx'],f.filename))
            #     print("saved text file name ---> ",f.filename)

            # elif(fName.endswith(".pdf")):
            #     f.save(os.path.join(app.config['UPLOAD_PATH_pdf'],f.filename))
            #     print("saved text file name ---> ",f.filename)

                #time.sleep(3)
                #os.system("java -version")
                #os.system("javac -version")

                #os.system("cd src/contracts && javac -cp ../../src/:../../jars/* -d ../../bin2 -encoding ISO-8859-1 GetClientDocs.java && cd ../../bin2/ && java -cp ../bin2/:../jars/* contracts.GetClientDocs "+ f.filename) 
                



        # test = os.listdir("upload")
        # for item in test:
        #     if item.endswith(".zip"):
        #         os.remove(os.path.join("upload", item))

        test = os.listdir("upload")
        for item in test:
            print('type of item var: ', type(item))
            if item.endswith(".txt"):
                print("text file name item",item)
                os.system("cd src/contracts && javac -cp ../../src/:../../jars/* -d ../../bin2 -encoding ISO-8859-1 GetClientDocs.java && cd ../../bin2/ && java -cp ../bin2/:../jars/* contracts.GetClientDocs "+ isPrivate + ' ' + item )
                # D suggestions:
                # os.system("cd src/contracts && javac -cp ../../src/:../../jars/* -d ../../bin2 -encoding ISO-8859-1 GetClientDocs.java && cd ../../bin2/ && java -cp ../bin2/:../jars/* contracts.GetClientDocs "+ "\"" + item + "\"" )

            
            elif item.endswith(".docx"):
            # Usage
                import docx
                from simplify_docx import simplify

                # read in a document 
                my_doc = docx.Document("/path/to/my/favorite/file.docx")

                # coerce to JSON using the standard options
                my_doc_as_json = simplify(my_doc)

                # or with non-standard options
                my_doc_as_json = simplify(my_doc,{"remove-leading-white-space":False})
                print("text file name item",item)
                os.system("cd src/contracts && javac -cp ../../src/:../../jars/* -d ../../bin2 -encoding ISO-8859-1 GetClientDocs.java && cd ../../bin2/ && java -cp ../bin2/:../jars/* contracts.GetClientDocs "+ item )

            # elif item.endswith(".pptx"):
            #     print("text file name item",item)
            #     os.system("cd src/contracts && javac -cp ../../src/:../../jars/* -d ../../bin2 -encoding ISO-8859-1 GetClientDocs.java && cd ../../bin2/ && java -cp ../bin2/:../jars/* contracts.GetClientDocs "+ item)

            # elif item.endswith(".pdf"):
            #     print("text file name item",item)
            #     os.system("cd src/contracts && javac -cp ../../src/:../../jars/* -d ../../bin2 -encoding ISO-8859-1 GetClientDocs.java && cd ../../bin2/ && java -cp ../bin2/:../jars/* contracts.GetClientDocs "+ item)


        print("Deleting the cotents from upload volume -----------><")
        test = os.listdir("upload")
        for item in test:
            os.remove(os.path.join("upload", item))


        #gif here:        
        return render_template("upload-file.html",msg="Please upload file here:", sup="Files uploaded successfully...", flist=flist, off="True")
    return render_template("upload-file.html", msg="Please upload file here:")













# def upload_file():
#     flist = []
#     if request.method == 'POST':
#         for f in request.files.getlist('file_name'):
#             # f=request.files['file_name']
#             fName = f.filename
#             flist.append(fName)
#             print("list of docs and zips",flist)
#             if(fName.endswith(".zip")):
#                 print(f.filename)
#                 f.save(os.path.join(app.config['PATH_zip'],f.filename))
#                 fName = app.config['PATH_zip'] + '/' + fName
#                 fpath = app.config['UPLOAD_PATH_zip']
#                 #print("here... fname is:", fName)
#                 #print("here... path is:", fpath)
#                 ext(fName, fpath)

#                 test = os.listdir("upload")
#                 for item in test:
#                     if item.endswith(".zip"):
#                         os.remove(os.path.join("upload", item))

#                 #test = os.listdir("upload")
#                 #for item in test:
#                 #    os.system("cd src/contracts && javac -cp ../../src/:../../jars/* -d ../../bin2 -encoding ISO-8859-1 GetClientDocs.java && cd ../../bin2/ && java -cp ../bin2/:../jars/* contracts.GetClientDocs "+ f.filename) 




#             elif (fName.endswith(".txt")):

#                 print(f.filename)
#                 f.save(os.path.join(app.config['UPLOAD_PATH_txt'],f.filename))
#             print("***********",f.filename)
#             time.sleep(3)
#             #os.system("java -version")
#             #os.system("javac -version")

#             os.system("cd src/contracts && javac -cp ../../src/:../../jars/* -d ../../bin2 -encoding ISO-8859-1 GetClientDocs.java && cd ../../bin2/ && java -cp ../bin2/:../jars/* contracts.GetClientDocs "+ f.filename) 
            
            
#         #gif here:        
#         return render_template("upload-file.html",msg="Please upload file here:", sup="Files uploaded successfully...", flist=flist, off="True")
#     return render_template("upload-file.html", msg="Please upload file here:")

def ext(name, path):
    # name='C:/0.RUSH/Flairminds/zips/zips.zip'
    # open the zip file
    with ZipFile(name, 'r') as handle:
        # extract all files
        handle.extractall(path)

if __name__ == '__main__':
    print("http://localhost:8082/upload_file")
    print("Serving...")
    serve(app, host="0.0.0.0", port=8082)
    # app.run(debug=True)
    

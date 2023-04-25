FROM openjdk:11

RUN apt-get update -y

RUN apt-get install -y curl

RUN apt-get install -y python

RUN apt-get install -y pip

#COPY requirements.txt .

#RUN pip install --upgrade pip && pip install -r requirements.txt

RUN pip install --trusted-host pypi.python.org flask

RUN pip install waitress

RUN pip install docx2txt

#RUN pip install zipfile

#ENV JAVA_HOME="/usr/lib/jvm/java-8-openjdk"

ENV JAVA_HOME="/usr/lib/jvm/java-1.8-openjdk"
ENV PATH="$JAVA_HOME/bin:${PATH}"

ENV PYTHON_HOME = "/usr/lib/python3.9"

#RUN python -version
RUN java -version
RUN javac -version

####

EXPOSE 8082:8082
COPY . /
CMD ["python3.9", "upload.py"]

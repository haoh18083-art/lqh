FROM python:3.11-slim

WORKDIR /app

COPY langgraph_app/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY langgraph_app/ .

ENV PYTHONUNBUFFERED=1

CMD ["uvicorn", "api.server:app", "--host", "0.0.0.0", "--port", "8001"]

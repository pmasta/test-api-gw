Prerequisites
Make sure you have the following installed:

Python 3.8 or higher

pip (Python package manager)

(Optional) virtualenv or venv for isolated environments

1. Set up the environment
bash
Kopiuj
Edytuj
# Navigate to the Streamlit app directory
cd streamlit-app

# (Optional) Create and activate a virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
2. Install Python dependencies
Before running the app, you must install all required Python libraries listed in requirements.txt:

bash
Kopiuj
Edytuj
pip install -r requirements.txt
This step is mandatory to ensure the app runs correctly.

3. Run the app
bash
Kopiuj
Edytuj
streamlit run app.py
Replace app.py with the actual filename if different.

4. Access the app
Once started, the app will be available at:

arduino
Kopiuj
Edytuj
http://localhost:8501

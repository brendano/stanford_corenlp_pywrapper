from setuptools import setup

setup(
    name='stanford_corenlp_pywrapper',

    version='0.1.0',

    description='A Python wrapper for CoreNLP.',

    packages=['stanford_corenlp_pywrapper'],
    package_dir={'stanford_corenlp_pywrapper': 'stanford_corenlp_pywrapper'},
    package_data={
        'stanford_corenlp_pywrapper': ['lib/*'],
    }
)

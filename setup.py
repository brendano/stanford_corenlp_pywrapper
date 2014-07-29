from setuptools import setup, find_packages  # Always prefer setuptools over distutils

setup(
    name='stanford_corepywrapper',

    version='0.0.1a',

    description='A Python wrapper for CoreNLP.',

    package_data={
        'wrapper': ['javasrc/*', 'lib/*'],
    }
)

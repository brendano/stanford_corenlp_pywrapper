from setuptools import setup, find_packages  # Always prefer setuptools over distutils

setup(
    name='stanford-corepywrapper',

    version='0.0.1a',

    description='A Python wrapper for CoreNLP.',

    package_data={
        'petrarch': ['javasrc/*', 'lib/*'],
    }
)

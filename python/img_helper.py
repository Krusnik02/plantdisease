import os
from PIL import Image
import matplotlib.pyplot as plt
import glob
import numpy as np
import os, os.path



# функции импорта
def prepare_image(im, shape):
    # resize to minimum size
    if im.size[0] < shape[0]:
        im.thumbnail((shape[0], im.size[1]), Image.ANTIALIAS)
    if im.size[1] < shape[1]:
        im.thumbnail((im.size[0], shape[1]), Image.ANTIALIAS)

    # image is sqare
    img_res = []
    width, height = im.size
    if width == height:
        img_res.append(im)
    elif width > height: # dlinny
        img_res.append(im.crop((0, 0, height, height))) # left side
        img_res.append(im.crop((width - height, 0, width, height)))  # right side
    else: # width < height visoky:
        img_res.append(im.crop((0, 0, width, width))) # left side
        img_res.append(im.crop((0, height - width, width, height)))  # right side

    # general prepare
    imgs = []
    for img in img_res:
        resized_image = img.resize((shape[0], shape[1]), Image.ANTIALIAS)
        np_img = np.array(resized_image)
        np_img = np_img.reshape(shape[0], shape[1], shape[2])
        # np_img = np_img / 127.5 - 1
        np_img = np_img / 255.0

        imgs.append(np_img)

    # return
    return imgs

def inputFiles(input_folder):
    return glob.glob(input_folder + '\\*.*', recursive=False)


def read_image(filename, shape):
    #print(filename)
    # img_name = "%s/%s" % (input_folder, filename)
    img_name = filename

    # загружаем
    try:
        original_image = Image.open(img_name)
    except IOError:
        return None

    # возвращаем
    return prepare_image(original_image, shape)


def read_images(files, shape):
    imgs = []
    classes_list = []
    for f in files:
        im = read_image(f, shape)
        #print(f)
        if im is not None:
            imgs += im

    return imgs


def read_path_plant(input_folder, name, disease, part, shape):
    files = ''
    if part != None:
        files = inputFiles(os.path.join(input_folder, name, disease, part))
    else:
        files = inputFiles(os.path.join(input_folder, name, disease))
    return read_images(files, shape)

def read_path(input_folder, shape):
    files = inputFiles(os.path.join(input_folder))
    return read_images(files, shape)

def drow_image(img):
    plt.imshow(img)
    plt.show()
